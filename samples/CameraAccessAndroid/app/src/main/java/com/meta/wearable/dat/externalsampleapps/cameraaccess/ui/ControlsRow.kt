package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ControlsRow(
    onStopStream: () -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleAI: () -> Unit,
    isAIActive: Boolean,
    onToggleLive: () -> Unit,
    isLiveActive: Boolean,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Present whenever the caller wires it. The phone screen is the app's
        // front door and carries no top bar, so without this there is no way
        // back into Settings once the call controls replace the home screen.
        if (onOpenSettings != null) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.aspectRatio(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.DeepBlue),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                )
            }
        }

        SwitchButton(
            label = "Stop",
            onClick = onStopStream,
            isDestructive = true,
            modifier = Modifier.weight(1f),
        )

        CaptureButton(
            onClick = onCapturePhoto,
        )

        // AI toggle button
        Button(
            onClick = onToggleAI,
            modifier = Modifier.aspectRatio(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAIActive) AppColor.Green else AppColor.DeepBlue,
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = if (isAIActive) "Stop AI" else "Start AI",
                tint = Color.White,
            )
        }

        // Live toggle button
        Button(
            onClick = onToggleLive,
            modifier = Modifier.aspectRatio(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLiveActive) AppColor.Red else AppColor.DeepBlue,
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = if (isLiveActive) "Stop Live" else "Start Live",
                tint = Color.White,
            )
        }
    }
}
