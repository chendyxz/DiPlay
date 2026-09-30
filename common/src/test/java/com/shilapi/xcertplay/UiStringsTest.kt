// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay

import org.junit.Assert.assertEquals
import org.junit.Test

class UiStringsTest {
    @Test
    fun chineseLocaleTranslatesStaticAndDynamicUiText() {
        assertEquals("连接手机", localizedUiText("Connect phone", "zh"))
        assertEquals("选择 iPhone · Alice", localizedUiText("Choose iPhone · Alice", "zh"))
    }

    @Test
    fun chineseLocaleTranslatesCarPlayLoadingPage() {
        assertEquals(
            "正在打开 CarPlay…",
            localizedUiText("Opening CarPlay…", "zh"),
        )
        assertEquals(
            "正在等待 iPhone 的 CarPlay 画面…",
            localizedUiText("Waiting for CarPlay video…", "zh"),
        )
        assertEquals(
            "iPhone 未通过 Wi-Fi 启动 CarPlay，正在重试…",
            localizedUiText("Your iPhone didn’t start CarPlay over Wi-Fi. Retrying…", "zh"),
        )
        assertEquals(
            "请将 iPhone 放在附近，并开启蓝牙和 Wi-Fi。\n在 iPhone 提示时允许 CarPlay。",
            localizedUiText(
                "Keep your iPhone nearby with Bluetooth and Wi-Fi on.\nAllow CarPlay if your iPhone asks.",
                "zh",
            ),
        )
        assertEquals("返回 DashFlow", localizedUiText("Back to DashFlow", "zh"))
        assertEquals(
            "在 CarPlay 中，三指向下滑动即可打开 DashFlow 设置。",
            localizedUiText(
                "In CarPlay, swipe down with three fingers to open DashFlow settings.",
                "zh",
            ),
        )
    }

    @Test
    fun otherLocalesKeepEnglishUiText() {
        assertEquals("Connect phone", localizedUiText("Connect phone", "en"))
        assertEquals("Opening CarPlay…", localizedUiText("Opening CarPlay…", "en"))
    }
}
