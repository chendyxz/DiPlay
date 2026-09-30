// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay

import android.content.Context
import android.os.Build
import java.util.Locale

/** Chinese copy for the programmatically-built UI. English remains the default locale. */
internal fun Context.uiText(english: String): String {
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        resources.configuration.locale
    }
    return localizedUiText(english, locale.language)
}

internal fun localizedUiText(english: String, language: String): String {
    if (language != Locale.CHINESE.language) return english
    UI_ZH[english]?.let { return it }
    return translateDynamicUiText(english)
}

private fun translateDynamicUiText(text: String): String = when {
    text.startsWith("PUBLIC PREVIEW  ·  ") -> "公开预览版  ·  ${text.substringAfter("PUBLIC PREVIEW  ·  ")}"
    text.startsWith("Public preview · ") -> "公开预览版 · ${text.substringAfter("Public preview · ")}"
    text.startsWith("Choose iPhone · ") -> "选择 iPhone · ${text.substringAfter("Choose iPhone · ")}"
    text.startsWith("Edit saved hotspot · ") -> "编辑已保存的热点 · ${text.substringAfter("Edit saved hotspot · ")}"
    text.startsWith("Ready for ") -> "已准备连接 ${text.substringAfter("Ready for ")}"
    text.startsWith("The car hotspot “") -> text.replaceFirst("The car hotspot “", "车机热点“")
        .replace("” is off. Turn it on in the car settings before connecting.", "”已关闭。请先在车机设置中开启热点。")
    text.startsWith("DashFlow connects through the car hotspot “") -> text
        .replaceFirst("DashFlow connects through the car hotspot “", "DashFlow 通过车机热点“")
        .replace("”. Turn it on in the car settings, then connect.", "”连接。请在车机设置中开启热点后再连接。")
    text.startsWith("Downloads/DashFlow/") -> text
    text.startsWith("Left ") -> text.replaceFirst("Left", "左移")
    text.startsWith("Right ") -> text.replaceFirst("Right", "右移")
    text.startsWith("Up ") -> text.replaceFirst("Up", "上移")
    text.startsWith("Down ") -> text.replaceFirst("Down", "下移")
    else -> text
}

private val UI_ZH = mapOf(
    "Keep your iPhone nearby with Bluetooth and Wi-Fi on.\nAllow CarPlay if your iPhone asks." to "请将 iPhone 放在附近，并开启蓝牙和 Wi-Fi。\n在 iPhone 提示时允许 CarPlay。",
    "Use a USB data cable and unlock your iPhone.\nAllow Trust and CarPlay if your iPhone asks." to "请使用 USB 数据线连接，并解锁 iPhone。\n在 iPhone 提示时选择“信任”并允许 CarPlay。",
    "In CarPlay, swipe down with three fingers to open DashFlow settings." to "在 CarPlay 中，三指向下滑动即可打开 DashFlow 设置。",
    "Turn on Wi-Fi in the head unit’s settings to connect." to "请在车机设置中开启 Wi-Fi 后连接。",
    "Allow precise Location for DashFlow in the head unit’s app permissions." to "请在车机应用权限中允许 DashFlow 使用精确位置。",
    "Allow Nearby devices for DashFlow in the head unit’s app permissions." to "请在车机应用权限中允许 DashFlow 访问附近设备。",
    "The head unit couldn’t start CarPlay Wi-Fi. Check Wi-Fi and close other projection apps. Retrying…" to "车机无法启动 CarPlay Wi-Fi。请检查 Wi-Fi 并关闭其他投屏应用。正在重试…",
    "A previous Wi-Fi Direct connection is still running. Reset it to connect." to "上次的 Wi-Fi Direct 连接仍在运行，请重置后再连接。",
    "Your iPhone isn’t available. Unlock it and check Bluetooth." to "暂时无法连接 iPhone，请解锁手机并检查蓝牙。",
    "This head unit may not support wireless CarPlay. Try a USB connection." to "此车机可能不支持无线 CarPlay，请尝试通过 USB 连接。",
    "Allow the connection permission to continue" to "请允许连接权限以继续",
    "Connection interrupted. Retrying…" to "连接已中断，正在重试…",
    "Connect your iPhone with a USB cable" to "请使用 USB 数据线连接 iPhone",
    "Looking for your paired iPhone…" to "正在查找已配对的 iPhone…",
    "Reconnecting to your iPhone…" to "正在重新连接 iPhone…",
    "Opening CarPlay…" to "正在打开 CarPlay…",
    "Car home" to "车机主页", "Back" to "返回", "YOUR PHONE. YOUR DRIVE." to "你的手机，你的旅程。",
    "A familiar drive." to "熟悉的驾乘体验。", "Your maps, music and conversations.\nCarPlay, right here on your car display." to "地图、音乐与通话，\n都在车载屏幕上的 CarPlay 中。",
    "WIRELESS CARPLAY" to "无线 CARPLAY", "Ready when you are" to "随时可以连接", "Connect phone" to "连接手机",
    "Built-in car hotspot · Keep the car hotspot, Bluetooth and your iPhone’s Wi-Fi on." to "车机内置热点 · 请保持车机热点、蓝牙和 iPhone Wi-Fi 开启。",
    "Open Connection setup to save your built-in car hotspot details." to "请打开连接设置，保存车机内置热点信息。",
    "Wi-Fi Direct · Keep the car’s Wi-Fi switch, Bluetooth and your iPhone’s Wi-Fi on." to "Wi-Fi Direct · 请保持车机 Wi-Fi、蓝牙和 iPhone Wi-Fi 开启。",
    "Open car hotspot settings" to "打开车机热点设置", "Choose iPhone" to "选择 iPhone", "Disconnect" to "断开连接",
    "Connect with USB" to "通过 USB 连接", "Plug your iPhone into a USB data port.\nAllow CarPlay when your iPhone asks." to "将 iPhone 接入 USB 数据接口。\n在 iPhone 提示时允许 CarPlay。",
    "Settings" to "设置", "Make DashFlow feel right for your car." to "按你的车机调整 DashFlow。",
    "Your drive, your way." to "按你的方式驾驶。", "Apply reconnects CarPlay for size, resolution, music buffer and frame rate. Other changes apply to your next connection." to "应用尺寸、分辨率、音乐缓冲和帧率设置时会重新连接 CarPlay；其他更改将在下次连接时生效。",
    "Connection setup" to "连接设置", "Choose how to connect, follow the setup steps and save your car hotspot details." to "选择连接方式，按步骤完成设置并保存车机热点信息。", "Open connection setup" to "打开连接设置",
    "Diagnostics" to "诊断", "Saving report…" to "正在保存报告…", "Save diagnostic report" to "保存诊断报告", "Choose save location" to "选择保存位置",
    "Reports save to Downloads/DashFlow. " to "报告将保存到 Downloads/DashFlow。", "Choose where to save your report. " to "请选择报告保存位置。", "Nothing is sent automatically. Protocol payloads and credentials are excluded." to "不会自动发送任何内容，报告不包含协议数据和凭据。",
    "Automatic connection" to "自动连接", "Connect when DashFlow opens" to "打开 DashFlow 时连接", "Use your last connection type and selected iPhone." to "使用上次的连接方式和所选 iPhone。", "Open after the car starts" to "车机启动后打开", "Availability depends on your head unit’s startup settings." to "是否可用取决于车机的启动设置。",
    "Display and performance" to "显示与性能", "Resolution" to "分辨率", "Native" to "原始分辨率", "80% · lighter load" to "80% · 较低负载", "60% · lightest load" to "60% · 最低负载",
    "Music buffer" to "音乐缓冲", "300 ms · default" to "300 毫秒 · 默认", "500 ms" to "500 毫秒", "1000 ms · most stable" to "1000 毫秒 · 最稳定", "Frame rate" to "帧率", "30 fps · lighter load" to "30 帧 · 较低负载", "60 fps · smoother motion" to "60 帧 · 更流畅",
    "Efficient video" to "高效视频", "Use HEVC. Leave off for the widest head-unit compatibility." to "使用 HEVC。关闭可获得更广泛的车机兼容性。", "Right-hand drive" to "右舵车", "Place CarPlay’s controls closer to the driver." to "将 CarPlay 控件放在更靠近驾驶员的一侧。", "Full screen" to "全屏", "Hide the car’s system bars while CarPlay is open." to "CarPlay 打开时隐藏车机系统栏。",
    "BYD navigation" to "比亚迪导航", "Navigation on HUD and instrument cluster" to "在 HUD 和仪表上显示导航", "Show phone navigation arrows, distance and street names on supported BYD displays. Vehicle compatibility varies." to "在支持的比亚迪屏幕上显示手机导航箭头、距离和道路名称，具体兼容性因车型而异。",
    "CarPlay map on instrument cluster · experimental" to "仪表显示 CarPlay 地图 · 实验性", "Follow instrument theme and map card" to "跟随仪表主题和地图卡片", "Usage Access: enabled" to "使用情况访问权限：已启用", "Usage Access: setup needed for automatic mode" to "使用情况访问权限：自动模式需要设置", "Automatic map setup · ADB" to "自动地图设置 · ADB",
    "Instrument theme" to "仪表主题", "Instrument contrast" to "仪表对比度", "Cluster map size" to "仪表地图大小", "Standard · sharpest" to "标准 · 最清晰", "Larger · default" to "较大 · 默认", "Largest" to "最大", "Car marker · horizontal" to "车辆标记 · 水平", "Car marker · vertical" to "车辆标记 · 垂直", "Centre · default" to "居中 · 默认", "Reset car marker to centre" to "将车辆标记恢复居中",
    "Permissions and connection help" to "权限与连接帮助", "Nearby devices connects your iPhone. Microphone enables Siri and calls. Older Android versions also require Location for wireless setup. USB mode may ask for a local VPN connection." to "附近设备权限用于连接 iPhone，麦克风权限用于 Siri 和通话。旧版 Android 的无线设置还需要位置权限；USB 模式可能会请求本地 VPN 连接。", "App permissions" to "应用权限", "Bluetooth settings" to "蓝牙设置", "Wireless connection help" to "无线连接帮助",
    "About" to "关于", "About DashFlow" to "关于 DashFlow", "CarPlay, at home in your car." to "让 CarPlay 融入你的车。", "Made possible by open source" to "由开源项目共同成就",
    "Car hotspot is off" to "车机热点已关闭", "Open car settings" to "打开车机设置", "Connect" to "连接", "Cancel" to "取消",
    "Set up once. Your details stay saved for the next drive. Changes apply to your next connection." to "只需设置一次，信息会保留到下次驾驶；更改将在下次连接时生效。", "1 · Choose your connection" to "1 · 选择连接方式", "2 · Pair your iPhone" to "2 · 配对 iPhone", "3 · Connect" to "3 · 连接", "Review app permissions" to "检查应用权限", "Prefer a cable?" to "更喜欢有线连接？",
    "Built-in car hotspot" to "车机内置热点", "Use the car’s own hotspot. Select 5 GHz in car settings if available." to "使用车机自身热点，如可用请在车机设置中选择 5 GHz。", "Alternative setup. Requires the car’s Wi-Fi switch on; a 2.4 GHz connection may stutter." to "备用连接方式，需要开启车机 Wi-Fi；2.4 GHz 连接可能会卡顿。", "Hotspot setup" to "热点设置", "Save hotspot details and use this mode" to "保存热点信息并使用此模式", "Open car Wi-Fi settings" to "打开车机 Wi-Fi 设置",
    "Hotspot name" to "热点名称", "Hotspot password" to "热点密码", "Show password" to "显示密码", "Car hotspot details" to "车机热点信息", "Save details" to "保存信息", "Hide keyboard" to "收起键盘",
    "Save" to "保存", "Close" to "关闭", "Later" to "稍后", "Got it" to "知道了", "Done" to "完成", "Share" to "分享", "Choose location" to "选择位置", "App settings" to "应用设置", "Apply and reconnect" to "应用并重新连接",
    "Turn on Bluetooth" to "开启蓝牙", "Open Bluetooth" to "打开蓝牙", "Pair your iPhone" to "配对 iPhone", "Choose your iPhone" to "选择你的 iPhone", "Paired device" to "已配对设备", "Pair another" to "配对其他设备", "Reset CarPlay Wi-Fi" to "重置 CarPlay Wi-Fi", "Reset CarPlay Wi-Fi?" to "重置 CarPlay Wi-Fi？", "Reset and connect" to "重置并连接",
    "Setup needs attention" to "设置需要处理", "CarPlay connected" to "CarPlay 已连接", "Connecting to your iPhone…" to "正在连接 iPhone…", "Open CarPlay" to "打开 CarPlay",
    "Diagnostic report saved" to "诊断报告已保存", "Your report was saved to the selected location." to "报告已保存到所选位置。", "Could not save the report" to "无法保存报告", "Check that storage is available, or choose another save location." to "请检查存储空间是否可用，或选择其他保存位置。",
    "CarPlay size" to "CarPlay 尺寸", "Changes the size of CarPlay icons and text. Applying a size reconnects CarPlay." to "调整 CarPlay 图标和文字大小。应用后会重新连接 CarPlay。", "Saved for your next connection" to "已保存，将在下次连接时生效", "One-time setup on this car" to "在本车上完成一次性设置", "Copy command" to "复制命令", "Permission enabled · ready to use" to "权限已启用 · 可以使用", "Permission not enabled yet" to "权限尚未启用", "Automatic cluster map setup" to "自动仪表地图设置", "Check and enable" to "检查并启用"
    , "CarPlay settings" to "CarPlay 设置", "Back to DashFlow" to "返回 DashFlow", "EXIT APPLICATION" to "退出应用",
    "Wireless CarPlay" to "无线 CarPlay", "Hotspot status" to "热点状态", "Wireless hotspot: off" to "无线热点：已关闭",
    "Physical size basis" to "物理尺寸基准", "HEVC software decoder" to "HEVC 软件解码器", "Advanced audio channel mapping" to "高级音频声道映射",
    "AirPlay icon" to "AirPlay 图标", "Choose image" to "选择图片", "Default icon" to "默认图标", "Driving side" to "驾驶侧", "Left-hand drive" to "左舵车",
    "Hide top bar" to "隐藏顶部栏", "Hide bottom bar" to "隐藏底部栏", "Hide the status bar" to "隐藏状态栏", "Hide the navigation bar" to "隐藏导航栏",
    "Safe area" to "安全区域", "Set" to "设置", "Reset" to "重置", "Draw outside safe area" to "在安全区域外绘制", "Allow CarPlay UI outside the safe area" to "允许 CarPlay 界面显示在安全区域外",
    "Wi-Fi session" to "Wi-Fi 会话", "Hotspot SSID" to "热点 SSID", "Band" to "频段", "Auto" to "自动", "Channel (0 = auto)" to "信道（0 = 自动）", "Security" to "安全类型", "Open" to "开放",
    "Report location to iPhone" to "向 iPhone 报告位置", "Auto-start on boot" to "开机自动启动", "Start CarPlay automatically after device boot" to "设备启动后自动启动 CarPlay",
    "Debug logs" to "调试日志", "Show on-screen debug logs" to "在屏幕上显示调试日志", "Save and reconnect" to "保存并重新连接", "Getting CarPlay ready…" to "正在准备 CarPlay…",
    "CarPlay connection" to "CarPlay 连接", "CarPlay connection running" to "CarPlay 连接正在运行",
    "Nearby devices" to "附近设备", "Allow Nearby devices so DashFlow can connect to your paired iPhone." to "请允许附近设备权限，以便 DashFlow 连接已配对的 iPhone。",
    "Manual mode: match the cluster theme here. The map cannot follow card visibility without Usage Access." to "手动模式：请在此匹配仪表主题。没有使用情况访问权限时，地图无法跟随卡片显示状态。",
    "Keep Bluetooth and Wi-Fi on your iPhone. Pair with the car’s Bluetooth, then select your iPhone here. Allow Nearby devices when asked." to "请保持 iPhone 的蓝牙和 Wi-Fi 开启，与车机蓝牙配对后在此选择 iPhone，并在提示时允许附近设备权限。",
    "Return from car settings to DashFlow, then connect. Accept the CarPlay prompt on your iPhone. A car internet plan is not required; mobile data availability depends on your phone’s network settings." to "从车机设置返回 DashFlow 后连接，并在 iPhone 上接受 CarPlay 提示。无需车机流量套餐；移动数据是否可用取决于手机网络设置。",
    "Use a USB data cable and the car’s USB data port. Unlock your iPhone and allow CarPlay. No hotspot setup is needed." to "使用 USB 数据线连接车机的数据接口，解锁 iPhone 并允许 CarPlay，无需设置热点。",
    "Copy these from the car’s hotspot settings. Use 5 GHz if available. Saving here does not change the car’s hotspot." to "请从车机热点设置中准确复制以下信息，如可用请使用 5 GHz。此处保存不会更改车机热点。",
    "Enable the car’s Bluetooth and pair your iPhone first." to "请先开启车机蓝牙并配对 iPhone。", "On your iPhone, open Settings → Bluetooth and pair with the car. Then return to DashFlow and choose Connect phone." to "在 iPhone 上打开“设置 → 蓝牙”并与车机配对，然后返回 DashFlow 并选择“连接手机”。",
    "Pair your iPhone with the car’s Bluetooth, keep Wi-Fi on, and allow CarPlay on the iPhone. Close any other phone-projection app.\n\nIf a previous projection app left its connection running, reset CarPlay Wi-Fi below and connect again. Your car’s normal internet Wi-Fi stays on." to "将 iPhone 与车机蓝牙配对，保持 Wi-Fi 开启，并在 iPhone 上允许 CarPlay。请关闭其他手机投屏应用。\n\n如果其他投屏应用遗留了连接，请在下方重置 CarPlay Wi-Fi 后重新连接；车机正常的互联网 Wi-Fi 不受影响。",
    "This ends the existing Wi-Fi Direct connection, including one left behind after reinstalling. Close other projection apps first. Your car’s internet Wi-Fi stays on." to "这将结束现有的 Wi-Fi Direct 连接，包括重新安装后遗留的连接。请先关闭其他投屏应用；车机的互联网 Wi-Fi 不受影响。",
    "Save your hotspot details in Connection setup first" to "请先在连接设置中保存热点信息", "Save the name and password from the car’s hotspot settings first" to "请先保存车机热点设置中的名称和密码",
    "This head unit does not support Wi-Fi Direct." to "此车机不支持 Wi-Fi Direct。", "Wi-Fi Direct is still busy. Close the other projection app and try again." to "Wi-Fi Direct 仍被占用，请关闭其他投屏应用后重试。", "Could not reset Wi-Fi Direct. Close the other projection app and try again." to "无法重置 Wi-Fi Direct，请关闭其他投屏应用后重试。",
    "Wireless permissions" to "无线连接权限", "Allow Nearby devices and, on older Android versions, Location before resetting CarPlay Wi-Fi." to "重置 CarPlay Wi-Fi 前，请允许附近设备权限；旧版 Android 还需允许位置权限。", "Report saved. Open it from your file manager to share it." to "报告已保存，请从文件管理器打开并分享。", "Open this setting from your car’s Settings app." to "请从车机的设置应用中打开此项设置。",
    "CarPlay authentication could not be loaded. Install the complete DashFlow build over this app. No uninstall or hotspot change is needed." to "无法加载 CarPlay 认证。请直接覆盖安装完整的 DashFlow 版本，无需卸载或更改热点设置。",
    "An independent CarPlay receiver for Android head units. Wired and wireless connections run on the head unit, with local authentication. A standard iPhone can connect without a jailbreak, Mac, dongle or sign-in.\n\nThis preview uses an experimental accessory identity. Compatibility with every iPhone and head unit is still being tested. It is not an Apple-certified product." to "面向 Android 车机的独立 CarPlay 接收器。有线和无线连接均在车机本地运行并完成认证，普通 iPhone 无需越狱、Mac、转接盒或登录即可连接。\n\n此预览版使用实验性配件身份，仍在测试不同 iPhone 和车机的兼容性，并非 Apple 认证产品。",
    "Receiver based on xcertplay, licensed under GPL-3.0. DashFlow’s interface follows DiAuto’s design, licensed under AGPL-3.0.\n\nIncludes AndroidX, Bouncy Castle, JmDNS and SLF4J. Source and license notices accompany this release.\n\nCarPlay and the CarPlay icon belong to Apple Inc. DashFlow is an independent project." to "接收器基于 xcertplay，采用 GPL-3.0 许可；DashFlow 界面沿用 DiAuto 的设计，采用 AGPL-3.0 许可。\n\n包含 AndroidX、Bouncy Castle、JmDNS 和 SLF4J，源代码及许可声明随版本提供。\n\nCarPlay 及其图标归 Apple Inc. 所有，DashFlow 是独立项目。",
    "1. Open car hotspot settings, turn the hotspot on and select 5 GHz if available.\n2. Copy its name and password below exactly.\n3. Leave the car hotspot on when connecting. DashFlow sends the details over Bluetooth; your iPhone joins automatically." to "1. 打开车机热点设置并开启热点，如可用请选择 5 GHz。\n2. 在下方准确填写热点名称和密码。\n3. 连接时保持车机热点开启。DashFlow 会通过蓝牙发送热点信息，iPhone 将自动加入。",
    "Finish setup · Save your hotspot details to use this mode." to "完成设置 · 保存热点信息后即可使用此模式。", "Hotspot is off · Turn it on in car settings." to "热点已关闭 · 请在车机设置中开启。", "Details saved · Check that the car hotspot is on before connecting." to "信息已保存 · 连接前请确认车机热点已开启。",
    "Turn the car’s Wi-Fi switch on. Allow Location / Nearby devices if requested and enable Location when Android asks. If playback stutters, try the built-in car hotspot at 5 GHz." to "请打开车机 Wi-Fi，并按提示允许位置/附近设备权限；Android 要求时还需开启位置服务。如果播放卡顿，请尝试使用 5 GHz 车机内置热点。",
    "Copied to the car clipboard. Run the command on your computer." to "已复制到车机剪贴板，请在电脑上运行该命令。", "Automatic map enabled. Open the cluster map card or select Map theme." to "自动地图已启用，请打开仪表地图卡片或选择地图主题。"
)
