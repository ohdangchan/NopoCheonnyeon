package com.nopo.cheonnyeon.feature.diagnostics

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.nopo.cheonnyeon.NopoApp
import com.nopo.cheonnyeon.R
import com.nopo.cheonnyeon.core.designsystem.component.NopoButton
import com.nopo.cheonnyeon.core.designsystem.component.NopoButtonStyle
import com.nopo.cheonnyeon.core.designsystem.theme.NopoTheme
import com.nopo.cheonnyeon.domain.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 공식 사진(백년가게 상세 페이지) 시험: 사진이 있는 가게를 하나씩 넘겨 보며 실제로 뜨는지 확인한다. */
@Composable
fun DiagnosticsPhotoTab() {
    val c = NopoTheme.colors
    val context = LocalContext.current
    var stores by remember { mutableStateOf<List<Store>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var photoIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val dao = (context.applicationContext as NopoApp).container.database.storeDao()
        stores = withContext(Dispatchers.IO) { dao.all().map { it.toDomain() }.filter { it.photoUrls.isNotEmpty() } }
    }

    val list = stores
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(NopoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(NopoTheme.spacing.md),
    ) {
        if (list == null) {
            Text(stringResource(R.string.diag_photo_loading), color = c.ink)
            return@Column
        }
        if (list.isEmpty()) {
            Text(stringResource(R.string.diag_photo_none), color = c.ink)
            return@Column
        }
        val store = list[index.coerceIn(0, list.lastIndex)]
        Text(stringResource(R.string.diag_photo_count, list.size), color = c.ink2)
        Text(store.name, style = NopoTheme.typography.titleLarge, color = c.ink)
        Text(store.address.ifBlank { store.sido + " " + store.sigungu }, color = c.ink2)
        Text(
            stringResource(R.string.diag_photo_position, index + 1, list.size, photoIndex + 1, store.photoUrls.size),
            style = NopoTheme.extraTypography.metric,
            color = c.ink,
        )
        SubcomposeAsyncImage(
            model = store.photoUrls[photoIndex.coerceIn(0, store.photoUrls.lastIndex)],
            contentDescription = stringResource(R.string.cd_diag_photo, store.name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            loading = { Text(stringResource(R.string.diag_photo_loading), color = c.ink2) },
            error = { Text(stringResource(R.string.diag_photo_error), color = c.ink) },
            onError = { Log.e("Nopo", "사진 불러오기 실패: " + it.result.throwable) },
            success = { SubcomposeAsyncImageContent() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NopoTheme.spacing.sm)) {
            NopoButton(
                text = stringResource(R.string.diag_photo_prev_store),
                onClick = { index = (index - 1 + list.size) % list.size; photoIndex = 0 },
                style = NopoButtonStyle.Outline,
                modifier = Modifier.weight(1f),
            )
            NopoButton(
                text = stringResource(R.string.diag_photo_next_store),
                onClick = { index = (index + 1) % list.size; photoIndex = 0 },
                modifier = Modifier.weight(1f),
            )
        }
        NopoButton(
            text = stringResource(R.string.diag_photo_next_photo),
            onClick = { photoIndex = (photoIndex + 1) % store.photoUrls.size },
            style = NopoButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
