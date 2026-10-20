package com.sift.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sift.data.Snippet
import com.sift.engine.SiftEngine
import com.sift.pipeline.Labels

// The card catalog: drawers with counts, cards with titles over verbatim
// text. Nothing here asks the user to do anything.
@Composable
fun CatalogScreen(
    snippets: List<Snippet>,
    unfiledCount: Int,
    filing: Boolean,
    modelStatus: SiftEngine.ModelStatus,
    onFileNow: () -> Unit,
    onGetModelFile: () -> Unit,
    onPickModelFile: () -> Unit,
) {
    val filed = snippets.filter { it.status == "FILED" }
    val byCategory = remember(snippets) { filed.groupBy { it.category ?: Labels.INBOX } }
    val unfiled = remember(snippets) { snippets.filter { it.status == "UNFILED" } }

    LazyColumn(modifier = Modifier.fillMaxSize().background(SiftColors.Surface)) {
        item { Header(unfiledCount = unfiled.size, filedCount = filed.size) }

        if (modelStatus != SiftEngine.ModelStatus.READY) {
            item { SetupCard(modelStatus, onGetModelFile, onPickModelFile) }
        }

        if (unfiled.isNotEmpty() || filing) {
            item {
                DrawerHeader(
                    label = "UNFILED",
                    count = unfiled.size,
                    accent = false,
                    action = if (unfiled.isNotEmpty() && !filing && modelStatus == SiftEngine.ModelStatus.READY) {
                        { FileNowPill(onFileNow) }
                    } else null,
                )
            }
            if (filing) item { SiftingRow() }
            items(unfiled.size) { i -> UnfiledRow(unfiled[i]) }
        }

        for (drawer in Labels.TAXONOMY) {
            val cards = byCategory[drawer].orEmpty()
            item { DrawerHeader(label = drawer, count = cards.size, accent = cards.isNotEmpty()) }
            items(cards.size) { i -> Card(cards[i]) }
        }

        val inbox = byCategory[Labels.INBOX].orEmpty()
        item { DrawerHeader(label = "INBOX", count = inbox.size, accent = false, quiet = true) }
        items(inbox.size) { i -> Card(inbox[i], quiet = true) }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun Header(unfiledCount: Int, filedCount: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SIFT", style = SiftType.wordmark, color = SiftColors.Ink)
            Spacer(Modifier.weight(1f))
            val statusText = when {
                unfiledCount > 0 -> "$unfiledCount kept · $filedCount filed"
                else -> "$filedCount filed"
            }
            Text(
                statusText,
                style = SiftType.status,
                color = SiftColors.InkSoft,
            )
        }
        Spacer(Modifier.height(12.dp))
        // The catalog's header signature: the double rule.
        Box(Modifier.fillMaxWidth().height(1.dp).background(SiftColors.Ink))
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SiftColors.Ink))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DrawerHeader(
    label: String,
    count: Int,
    accent: Boolean,
    quiet: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = SiftType.drawerLabel,
                color = if (quiet) SiftColors.InkSoft else SiftColors.Ink,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "$count",
                style = SiftType.count,
                color = if (accent) SiftColors.Pine else SiftColors.InkSoft,
            )
            Spacer(Modifier.weight(1f))
            action?.invoke()
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SiftColors.Rule))
    }
}

@Composable
private fun FileNowPill(onFileNow: () -> Unit) {
    Box(
        Modifier
            .background(SiftColors.PineWash, RectangleShape)
            .clickable(onClick = onFileNow)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("FILE NOW", style = SiftType.button, color = SiftColors.Pine)
    }
}

@Composable
private fun SiftingRow() {
    val transition = rememberInfiniteTransition(label = "sifting")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp).alpha(alpha),
        horizontalArrangement = Arrangement.End,
    ) {
        Text("SIFTING", style = SiftType.drawerLabel, color = SiftColors.Pine)
    }
}

@Composable
private fun UnfiledRow(snippet: Snippet) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            snippet.text,
            style = SiftType.snippetText,
            color = SiftColors.Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SiftColors.Rule))
    }
}

@Composable
private fun Card(snippet: Snippet, quiet: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            snippet.title ?: snippet.text,
            style = SiftType.snippetTitle,
            color = if (quiet) SiftColors.InkSoft else SiftColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            snippet.text,
            style = SiftType.snippetText,
            color = SiftColors.InkSoft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SiftColors.Rule))
    }
}

@Composable
private fun SetupCard(
    modelStatus: SiftEngine.ModelStatus,
    onGetModelFile: () -> Unit,
    onPickModelFile: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(16.dp))
        when (modelStatus) {
            SiftEngine.ModelStatus.LOADING -> {
                Text(
                    "Reading the model file… (first read takes a few minutes)",
                    style = SiftType.snippetText,
                    color = SiftColors.InkSoft,
                )
            }
            else -> {
                Text(
                    "Sift reads with Gemma, on this phone — nothing is ever uploaded. " +
                        "Get the model file (~557 MB) from Google's Gemma release, then hand it over.",
                    style = SiftType.snippetText,
                    color = SiftColors.InkSoft,
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    Text(
                        "GET THE FILE",
                        style = SiftType.button,
                        color = SiftColors.Pine,
                        modifier = Modifier.clickable(onClick = onGetModelFile).padding(vertical = 6.dp),
                    )
                    Spacer(Modifier.width(24.dp))
                    Text(
                        "I HAVE THE FILE",
                        style = SiftType.button,
                        color = SiftColors.Ink,
                        modifier = Modifier.clickable(onClick = onPickModelFile).padding(vertical = 6.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SiftColors.Rule))
    }
}
