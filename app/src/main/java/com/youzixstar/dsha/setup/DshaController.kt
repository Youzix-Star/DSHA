package com.youzixstar.dsha.setup

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.youzixstar.dsha.data.AppPrefs
import com.youzixstar.dsha.termux.Scripts
import com.youzixstar.dsha.termux.TermuxBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用级状态与动作中心。
 *
 * 不引入 DI 框架：此类在界面入口处用 remember 创建一次，
 * 内部持有 Compose 状态，界面直接读取即可。
 */
class DshaController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = AppPrefs(context)

    /** 每次进入应用最多自动拉起一次服务，避免反复重试 */
    private var autoStartAttempted = false

    // ---- 环境检测 ----
    var termuxInstalled by mutableStateOf(TermuxBridge.isTermuxInstalled(context))
        private set
    var permissionGranted by mutableStateOf(TermuxBridge.isRunCommandPermissionGranted(context))
        private set
    var bridgeOk by mutableStateOf(false)
        private set
    var bridgeError by mutableStateOf<String?>(null)
        private set
    var checking by mutableStateOf(false)
        private set

    // ---- 运行环境状态 ----
    var repoCloned by mutableStateOf(false)
        private set
    var dshBinAvailable by mutableStateOf(false)
        private set
    var installDirReady by mutableStateOf(false)
        private set
    var dshVersion by mutableStateOf("-")
        private set
    var nodeVersion by mutableStateOf("-")
        private set
    var serverRunning by mutableStateOf(false)
        private set

    // ---- 任务状态 ----
    var busy by mutableStateOf(false)
        private set
    var busyLabel by mutableStateOf("")
        private set
    // ---- 偏好设置 ----
    var autoStart by mutableStateOf(prefs.autoStart)
        private set
    var keepScreenOn by mutableStateOf(prefs.keepScreenOn)
        private set
    var themeModeIndex by mutableStateOf(prefs.themeModeIndex)
        private set
    var developerMode by mutableStateOf(prefs.developerMode)
        private set
    var uaMode by mutableStateOf(prefs.uaMode)
        private set

    val setupLog = mutableStateListOf<String>()
    val consoleLog = mutableStateListOf<String>()

    /** 网页控制台输出。放在控制器上，调试信息面板才能一并看到。 */
    val webConsole = mutableStateListOf<String>()

    /** 桥接与 dsh 命令均可用，功能完整 */
    val ready: Boolean get() = bridgeOk && dshBinAvailable

    /** 卡在 Termux 的 allow-external-apps 开关上 */
    val blockedByAllowExternalApps: Boolean
        get() = TermuxBridge.isAllowExternalAppsError(bridgeError)

    fun updateAutoStart(value: Boolean) {
        prefs.autoStart = value
        autoStart = value
        if (value) maybeAutoStart()
    }

    fun updateKeepScreenOn(value: Boolean) {
        prefs.keepScreenOn = value
        keepScreenOn = value
    }

    fun updateThemeModeIndex(value: Int) {
        prefs.themeModeIndex = value
        themeModeIndex = value
    }

    fun updateUaMode(value: Int) {
        prefs.uaMode = value
        uaMode = value
    }

    fun updateDeveloperMode(value: Boolean) {
        prefs.developerMode = value
        developerMode = value
    }

    /** 供调试信息面板读取当前快照。 */
    fun statusSnapshot(): List<Pair<String, String>> = listOf(
        "Termux 已安装" to if (termuxInstalled) "是" else "否",
        "RUN_COMMAND 权限" to if (permissionGranted) "已授予" else "未授予",
        "命令桥接" to if (bridgeOk) "已连通" else "未连通",
        "桥接错误" to (bridgeError ?: "-"),
        "dsh 命令" to if (dshBinAvailable) "存在" else "缺失",
        "dsh 版本" to dshVersion,
        "Node 版本" to nodeVersion,
        "安装目录" to if (installDirReady) "存在" else "缺失",
        "脚本仓库" to if (repoCloned) "已克隆" else "未克隆",
        "Web 服务" to if (serverRunning) "运行中" else "已停止",
        "任务状态" to if (busy) busyLabel.ifBlank { "执行中" } else "空闲",
        "控制台行数" to consoleLog.size.toString(),
        "引导输出行数" to setupLog.size.toString(),
    )

    /** 重新检测环境与运行状态 */
    fun refresh() {
        if (checking) return
        scope.launch {
            checking = true
            try {
                termuxInstalled = TermuxBridge.isTermuxInstalled(context)
                permissionGranted = TermuxBridge.isRunCommandPermissionGranted(context)
                if (!termuxInstalled) {
                    bridgeOk = false
                    bridgeError = null
                    return@launch
                }
                if (!permissionGranted) {
                    bridgeOk = false
                    bridgeError = "尚未授予 RUN_COMMAND 权限"
                    return@launch
                }
                val probe = TermuxBridge.run(
                    context = context,
                    command = Scripts.probe(context),
                    label = "DSHA 环境检测",
                    timeoutMs = 20_000L,
                )
                if (probe.ok && probe.stdout.contains("dsha-bridge-ok")) {
                    bridgeOk = true
                    bridgeError = null
                    applyStatus(
                        TermuxBridge.run(
                            context = context,
                            command = Scripts.status(context),
                            label = "DSHA 读取状态",
                            timeoutMs = 30_000L,
                        ),
                    )
                    maybeAutoStart()
                } else {
                    bridgeOk = false
                    bridgeError = probe.errorText
                }
            } finally {
                checking = false
            }
        }
    }

    /** 状态已知且用户开启了自动启动时，拉起一次服务 */
    private fun maybeAutoStart() {
        if (!autoStart || autoStartAttempted) return
        if (busy || serverRunning || !dshBinAvailable) return
        autoStartAttempted = true
        appendSetup("> 自动启动 DSH 服务")
        startServer()
    }

    private fun applyStatus(result: TermuxBridge.Result) {
        val map = result.stdout.lineSequence()
            .mapNotNull { line ->
                val at = line.indexOf('=')
                if (at <= 0) null else line.take(at).trim() to line.substring(at + 1).trim()
            }
            .toMap()
        repoCloned = map["repo"] == "yes"
        dshBinAvailable = map["dsh_bin"] == "yes"
        installDirReady = map["install_dir"] == "yes"
        dshVersion = map["dsh_version"]?.takeIf { it.isNotBlank() } ?: "-"
        nodeVersion = map["node"]?.takeIf { it.isNotBlank() } ?: "-"
        serverRunning = map["server"] == "running"
    }

    // ---------------------------------------------------------------- 动作

    /** 安装 / 更新 DSHA 运行环境（克隆脚本仓库 + setup.sh） */
    fun installRuntime() = runSetupTask(
        label = "安装 DSHA 运行环境（首次约 5~15 分钟）",
        command = { Scripts.install(context) },
        timeoutMs = 40 * 60 * 1000L,
    )

    fun startServer() = runSetupTask(
        label = "启动 DSH 服务",
        command = { Scripts.start(context) },
        timeoutMs = 3 * 60 * 1000L,
    )

    fun stopServer() {
        // 用户主动停止后复位，使下次进入应用仍可按偏好自动启动
        autoStartAttempted = false
        runSetupTask(
            label = "停止 DSH 服务",
            command = { Scripts.stop(context) },
            timeoutMs = 60_000L,
        )
    }

    fun readServerLog() = runConsoleTask("读取 DSH 日志", Scripts.logs(context), 60_000L)

    /** 在 Termux 中执行用户输入的命令，输出进控制台 */
    fun sendCommand(raw: String) {
        val command = raw.trim()
        if (command.isEmpty() || busy) return
        busy = true
        busyLabel = "执行命令"
        appendConsole("\$ $command")
        scope.launch {
            try {
                val result = TermuxBridge.run(context, command, "DSHA 控制台", 3 * 60 * 1000L)
                if (result.combined.isNotBlank()) appendConsole(result.combined)
                if (!result.ok) appendConsole("[失败] ${result.errorText}")
            } finally {
                busy = false
                busyLabel = ""
            }
        }
    }

    fun clearConsole() {
        consoleLog.clear()
    }

    fun appendWebConsole(line: String) {
        webConsole.add(line)
        while (webConsole.size > 200) webConsole.removeAt(0)
    }

    fun clearWebConsole() {
        webConsole.clear()
    }

    private fun runSetupTask(label: String, command: () -> String, timeoutMs: Long) {
        if (busy) return
        busy = true
        busyLabel = label
        appendSetup("> $label")
        scope.launch {
            try {
                val result = TermuxBridge.run(context, command(), label, timeoutMs)
                if (result.combined.isNotBlank()) appendSetup(result.combined)
                if (!result.ok) appendSetup("[失败] ${result.errorText}")
            } finally {
                busy = false
                busyLabel = ""
                refresh()
            }
        }
    }

    private fun runConsoleTask(label: String, command: String, timeoutMs: Long) {
        if (busy) return
        busy = true
        busyLabel = label
        scope.launch {
            try {
                val result = TermuxBridge.run(context, command, label, timeoutMs)
                if (result.combined.isNotBlank()) appendConsole(result.combined)
                if (!result.ok) appendConsole("[失败] ${result.errorText}")
            } finally {
                busy = false
                busyLabel = ""
            }
        }
    }

    private fun appendSetup(line: String) {
        setupLog.add(line)
        trim(setupLog)
    }

    private fun appendConsole(line: String) {
        consoleLog.add(line)
        trim(consoleLog)
    }

    private fun trim(list: MutableList<String>) {
        while (list.size > 400) list.removeAt(0)
    }
}
