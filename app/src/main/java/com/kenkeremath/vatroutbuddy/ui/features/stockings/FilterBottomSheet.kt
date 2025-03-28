@file:OptIn(ExperimentalLayoutApi::class)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filters: StockingFilters,
    onFiltersChanged: (StockingFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onFiltersChanged(filters.copy(
                                isNationalForest = if (filters.isNationalForest == true) null else true
                            ))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filters.isNationalForest == true,
                        onCheckedChange = { checked ->
                            onFiltersChanged(filters.copy(
                                isNationalForest = if (checked) true else null
                            ))
                        }
                    )
                    Text(
                        text = "National Forest Water",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onFiltersChanged(filters.copy(
                                isHeritageDayWater = if (filters.isHeritageDayWater == true) null else true
                            ))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filters.isHeritageDayWater == true,
                        onCheckedChange = { checked ->
                            onFiltersChanged(filters.copy(
                                isHeritageDayWater = if (checked) true else null
                            ))
                        }
                    )
                    Text(
                        text = "Heritage Day Water",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onFiltersChanged(filters.copy(
                                isNsf = if (filters.isNsf == true) null else true
                            ))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filters.isNsf == true,
                        onCheckedChange = { checked ->
                            onFiltersChanged(filters.copy(
                                isNsf = if (checked) true else null
                            ))
                        }
                    )
                    Text(
                        text = "NSF",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onFiltersChanged(filters.copy(
                                isDelayedHarvest = if (filters.isDelayedHarvest == true) null else true
                            ))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filters.isDelayedHarvest == true,
                        onCheckedChange = { checked ->
                            onFiltersChanged(filters.copy(
                                isDelayedHarvest = if (checked) true else null
                            ))
                        }
                    )
                    Text(
                        text = "Delayed Harvest Water",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}