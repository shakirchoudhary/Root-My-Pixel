package com.alex193a.rootmypixel.feature.main

import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alex193a.rootmypixel.R
import com.alex193a.rootmypixel.data.ManagerCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
fun ManagerPackageCard(
    selectedPackage: String,
    selectedLabel: String,
    isInstalled: Boolean,
    onChangeClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChangeClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = selectedPackage, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.manager_card_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = selectedPackage,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isInstalled) {
                    Text(
                        text = stringResource(R.string.manager_not_found_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onChangeClick) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.manager_card_change))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun ManagerPickerDialog(
    candidates: List<ManagerCandidate>,
    allApps: List<ManagerCandidate>,
    loading: Boolean,
    currentPackage: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(currentPackage) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery.trim().lowercase() }
            .distinctUntilChanged()
            .debounce(120)
            .collect { debouncedQuery = it }
    }

    val suggestedPkgs = remember(candidates) { candidates.map { it.packageName }.toSet() }
    val filteredSuggested = remember(candidates, debouncedQuery) {
        candidates.filter { it.matches(debouncedQuery) }
    }
    val filteredApps = remember(allApps, debouncedQuery, suggestedPkgs) {
        allApps.filter { app ->
            app.packageName !in suggestedPkgs && app.matches(debouncedQuery)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp)
                .imePadding(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.manager_picker_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.manager_picker_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.manager_picker_search),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (loading && allApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.manager_picker_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        if (filteredSuggested.isNotEmpty()) {
                            stickyHeader {
                                SectionHeader(stringResource(R.string.manager_picker_suggested))
                            }
                            items(
                                items = filteredSuggested,
                                key = { "s-${it.packageName}" },
                                contentType = { "app" },
                            ) { candidate ->
                                AppRow(
                                    candidate = candidate,
                                    isSelected = selected == candidate.packageName,
                                    onClick = { selected = candidate.packageName },
                                )
                            }
                        }

                        stickyHeader {
                            SectionHeader(
                                stringResource(R.string.manager_picker_all_apps, filteredApps.size),
                            )
                        }

                        if (filteredApps.isEmpty()) {
                            item {
                                Text(
                                    text = if (allApps.isEmpty()) {
                                        stringResource(R.string.manager_picker_loading)
                                    } else {
                                        stringResource(R.string.manager_picker_no_match, searchQuery)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
                                )
                            }
                        } else {
                            items(
                                items = filteredApps,
                                key = { it.packageName },
                                contentType = { "app" },
                            ) { candidate ->
                                AppRow(
                                    candidate = candidate,
                                    isSelected = selected == candidate.packageName,
                                    onClick = { selected = candidate.packageName },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.manager_picker_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (selected.isNotBlank()) onConfirm(selected) },
                        enabled = selected.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.manager_picker_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

@Composable
private fun AppRow(
    candidate: ManagerCandidate,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = candidate.packageName, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = candidate.packageName,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RadioButton(selected = isSelected, onClick = null)
    }
}

private val appIconCache = LruCache<String, Drawable>(80)

/**
 * Loads the launcher icon off the UI thread. Only visible rows decode icons,
 * and recent ones stay in a small LRU cache across scroll.
 */
@Composable
private fun AppIcon(packageName: String, size: Dp) {
    val context = LocalContext.current
    var drawable by remember(packageName) {
        mutableStateOf<Drawable?>(appIconCache.get(packageName))
    }

    LaunchedEffect(packageName) {
        if (drawable != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.Default) {
            runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
        }
        if (loaded != null) {
            appIconCache.put(packageName, loaded)
            drawable = loaded
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val icon = drawable
        if (icon != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                    }
                },
                update = { it.setImageDrawable(icon) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = null,
                modifier = Modifier.size(size * 0.55f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ManagerCandidate.matches(query: String): Boolean {
    if (query.isEmpty()) return true
    return searchText.contains(query)
}
