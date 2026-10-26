package com.nopo.cheonnyeon.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.nopo.cheonnyeon.core.designsystem.mock.MockColors
import com.nopo.cheonnyeon.core.designsystem.mock.MockPic
import com.nopo.cheonnyeon.R

/** 시안의 하단 탭 줄: 화면마다 선택된 탭 모양이 다른 그림을 그대로 쓰고, 다섯 칸을 눌러 이동한다 */
@Composable
fun MockNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val res = listOf(
        R.drawable.mk_nav_home,
        R.drawable.mk_nav_explore,
        R.drawable.mk_nav_map,
        R.drawable.mk_nav_collection,
        R.drawable.mk_nav_my
    )
    val labels = listOf(R.string.tab_home, R.string.tab_explore, R.string.tab_map, R.string.tab_passport, R.string.tab_settings)
    Column(Modifier.fillMaxWidth().background(MockColors.Cream).navigationBarsPadding()) {
        Box {
            MockPic(res[selected], 760, 98)
            Row(Modifier.matchParentSize()) {
                for (i in 0..4) {
                    val label = stringResource(labels[i])
                    Box(
                        Modifier.weight(1f).fillMaxSize().clickable(role = Role.Tab) { onSelect(i) }
                            .semantics { contentDescription = label },
                    )
                }
            }
        }
    }
}

