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
    fun otherLocalesKeepEnglishUiText() {
        assertEquals("Connect phone", localizedUiText("Connect phone", "en"))
    }
}
