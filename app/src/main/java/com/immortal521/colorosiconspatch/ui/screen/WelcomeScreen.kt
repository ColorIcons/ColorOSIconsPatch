package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.data.CheckStatus
import com.immortal521.colorosiconspatch.data.ModuleOperationStatus
import com.immortal521.colorosiconspatch.data.RootImplementation

private enum class WelcomeStep {
    INTRO,
    ROOT,
    MODULE,
    XPOSED,
    COMPLETE
}

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    rootStatus: CheckStatus,
    moduleStatus: CheckStatus,
    xposedStatus: CheckStatus,
    rootImplementation: RootImplementation,
    onRequestRoot: () -> Unit,
    onInstallModule: () -> Unit,
    moduleInstallStatus: ModuleOperationStatus?,
    moduleInstallMessage: String?,
    onContinue: () -> Unit,
    onCheckXposed: () -> Unit
) {
    var introComplete by rememberSaveable { mutableStateOf(false) }
    val entered = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { entered.targetState = true }
    val step = when {
        !introComplete -> WelcomeStep.INTRO
        rootStatus == CheckStatus.PASSED &&
            moduleStatus == CheckStatus.PASSED &&
            xposedStatus == CheckStatus.PASSED -> WelcomeStep.COMPLETE
        rootStatus == CheckStatus.PASSED && moduleStatus == CheckStatus.PASSED -> WelcomeStep.XPOSED
        rootStatus == CheckStatus.PASSED -> WelcomeStep.MODULE
        else -> WelcomeStep.ROOT
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        AnimatedVisibility(
            visibleState = entered,
            enter = fadeIn(tween(500)) +
                slideInVertically(tween(500)) { it / 3 }
        ) {
            Icon(
                imageVector = when (step) {
                    WelcomeStep.INTRO -> Icons.Filled.Palette
                    WelcomeStep.ROOT -> Icons.Filled.Security
                    WelcomeStep.MODULE -> Icons.Filled.Extension
                    WelcomeStep.XPOSED -> Icons.Filled.Security
                    WelcomeStep.COMPLETE -> Icons.Filled.CheckCircle
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(88.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(350)) { it / 3 } + fadeIn(tween(350))) togetherWith
                    (slideOutHorizontally(tween(250)) { -it / 4 } + fadeOut(tween(250)))
            },
            label = "welcome_step"
        ) { currentStep ->
            StepContent(
                step = currentStep,
                rootStatus = rootStatus,
                moduleStatus = moduleStatus,
                xposedStatus = xposedStatus,
                rootImplementation = rootImplementation,
                onRequestRoot = onRequestRoot,
                onInstallModule = onInstallModule,
                moduleInstallStatus = moduleInstallStatus,
                moduleInstallMessage = moduleInstallMessage,
                onContinue = onContinue,
                onCheckXposed = onCheckXposed,
                onStart = { introComplete = true }
            )
        }
    }
}
}

@Composable
private fun StepContent(
    step: WelcomeStep,
    rootStatus: CheckStatus,
    moduleStatus: CheckStatus,
    xposedStatus: CheckStatus,
    rootImplementation: RootImplementation,
    onRequestRoot: () -> Unit,
    onInstallModule: () -> Unit,
    moduleInstallStatus: ModuleOperationStatus?,
    moduleInstallMessage: String?,
    onContinue: () -> Unit,
    onCheckXposed: () -> Unit,
    onStart: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (step) {
            WelcomeStep.INTRO -> {
                Text("让图标更协调", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "ColorOS Icons Patch 会检查运行环境，并管理 ColorOS 图标适配模块。",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Button(onClick = onStart, modifier = Modifier.padding(top = 24.dp)) {
                    Text("开始设置")
                }
            }
            WelcomeStep.ROOT -> {
                Text("授予 Root 权限", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "需要 Root 权限来管理图标模块。",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                StatusText(rootStatus, "${rootImplementation.displayName} Root 权限")
                ActionForStatus(
                    status = rootStatus,
                    actionLabel = "请求 Root 权限",
                    onClick = onRequestRoot
                )
            }
            WelcomeStep.MODULE -> {
                Text("检查图标模块", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "检查模块状态，并在需要时准备模块。",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                StatusText(moduleStatus, "ColorOSIconsPatch 模块")
                if (moduleStatus != CheckStatus.PASSED) {
                    Button(
                        onClick = onInstallModule,
                        modifier = Modifier.padding(top = 12.dp),
                        enabled = moduleStatus != CheckStatus.CHECKING
                    ) {
                        Text("准备并安装模块")
                    }
                }
                moduleInstallMessage?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (moduleInstallStatus) {
                            ModuleOperationStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                            ModuleOperationStatus.MANUAL_INSTALL_REQUIRED -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            WelcomeStep.XPOSED -> {
                Text("检查 LSPosed 激活状态", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "请在 LSPosed 中启用本应用，并勾选 ColorOS Launcher 作用域。",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                StatusText(xposedStatus, "LSPosed 模块激活")
                ActionForStatus(
                    status = xposedStatus,
                    actionLabel = "重新检查 LSPosed",
                    onClick = onCheckXposed
                )
            }
            WelcomeStep.COMPLETE -> {
                Text("准备完成", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Root 和图标模块均已准备好。",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Button(onClick = onContinue, modifier = Modifier.padding(top = 24.dp)) {
                    Text("进入主界面")
                }
            }
        }
    }
}

@Composable
private fun StatusText(status: CheckStatus, label: String) {
    val text = when (status) {
        CheckStatus.UNKNOWN -> "尚未检查"
        CheckStatus.CHECKING -> "检查中..."
        CheckStatus.PASSED -> "已通过"
        CheckStatus.FAILED -> "未通过"
    }
    Text(
        text = "$label：$text",
        style = MaterialTheme.typography.titleMedium,
        color = if (status == CheckStatus.FAILED) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 24.dp)
    )
}

@Composable
private fun ActionForStatus(
    status: CheckStatus,
    actionLabel: String,
    onClick: () -> Unit
) {
    if (status == CheckStatus.CHECKING) {
        CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
    } else if (status != CheckStatus.PASSED) {
        Button(onClick = onClick, modifier = Modifier.padding(top = 24.dp)) {
            Text(actionLabel)
        }
    }
}
