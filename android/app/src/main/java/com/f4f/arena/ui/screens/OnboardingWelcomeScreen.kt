package com.f4f.arena.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4f.arena.R
import com.f4f.arena.ui.components.PrimaryLimePillButton
import com.f4f.arena.ui.theme.F4FColors

@Composable
fun OnboardingWelcomeScreen(
    onSignUpClick: () -> Unit,
    onSignInClick: () -> Unit,
    onBrowseAsGuest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(F4FColors.HeroVerticalBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(F4FColors.ForestDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "F4F Arena Logo",
                        modifier = Modifier.size(86.dp)
                    )
                }

                Text(
                    text = "F4F ARENA",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )

                Text(
                    text = "One Community. One Passion.\nFootball4Fitness.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Get Ready for Match Day",
                        style = MaterialTheme.typography.titleLarge,
                        color = F4FColors.TextPrimary
                    )
                    Text(
                        text = "Claim your player profile with a quick 4-digit PIN to vote IN/OUT on match polls and view live squads.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = F4FColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    PrimaryLimePillButton(
                        text = "Claim Profile / Sign Up",
                        icon = Icons.Outlined.PersonAdd,
                        onClick = onSignUpClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = onSignInClick,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Login,
                            contentDescription = null,
                            tint = F4FColors.ForestDeep,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sign In (Player or Admin)",
                            fontWeight = FontWeight.SemiBold,
                            color = F4FColors.ForestDeep,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(onClick = onBrowseAsGuest) {
                        Text(
                            "Browse as Guest",
                            color = F4FColors.TextMuted,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
