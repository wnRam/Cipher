package uz.angrykitten.spygame.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import uz.angrykitten.spygame.ui.game.playing.GameScreen
import uz.angrykitten.spygame.ui.game.result.RoundResultScreen
import uz.angrykitten.spygame.ui.game.reveal.RoleRevealScreen
import uz.angrykitten.spygame.ui.game.voting.VotingScreen
import uz.angrykitten.spygame.ui.home.HomeScreen
import uz.angrykitten.spygame.ui.howtoplay.HowToPlayScreen
import uz.angrykitten.spygame.ui.offline.OfflineGameScreen
import uz.angrykitten.spygame.ui.offline.OfflineModeViewModel
import uz.angrykitten.spygame.ui.offline.OfflineResultScreen
import uz.angrykitten.spygame.ui.offline.OfflineRevealScreen
import uz.angrykitten.spygame.ui.offline.OfflineSetupScreen
import uz.angrykitten.spygame.ui.profile.ProfileScreen
import uz.angrykitten.spygame.ui.room.create.CreateRoomScreen
import uz.angrykitten.spygame.ui.room.join.JoinRoomScreen
import uz.angrykitten.spygame.ui.room.lobby.LobbyScreen
import uz.angrykitten.spygame.ui.scoreboard.ScoreboardScreen
import uz.angrykitten.spygame.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val CREATE_ROOM = "create_room"
    const val LOBBY = "lobby/{isHost}"
    const val JOIN_ROOM = "join_room"
    const val ROLE_REVEAL = "role_reveal"
    const val GAME = "game"
    const val VOTING = "voting"
    const val ROUND_RESULT = "round_result"
    const val SCOREBOARD = "scoreboard"
    const val HOW_TO_PLAY = "how_to_play"
    const val SETTINGS = "settings"

    // Offline Mode
    const val OFFLINE_GRAPH = "offline_graph"
    const val OFFLINE_SETUP = "offline/setup"
    const val OFFLINE_REVEAL = "offline/reveal"
    const val OFFLINE_GAME = "offline/game"
    const val OFFLINE_RESULT = "offline/result"

    fun lobby(isHost: Boolean) = "lobby/$isHost"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToCreateRoom = { navController.navigate(Routes.CREATE_ROOM) },
                onNavigateToJoinRoom = { navController.navigate(Routes.JOIN_ROOM) },
                onNavigateToOfflineMode = { navController.navigate(Routes.OFFLINE_GRAPH) },
                onNavigateToHowToPlay = { navController.navigate(Routes.HOW_TO_PLAY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.CREATE_ROOM) {
            CreateRoomScreen(
                onNavigateBack = { navController.popBackStack() },
                onRoomCreated = {
                    navController.navigate(Routes.lobby(true)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.LOBBY) { backStackEntry ->
            val isHost = backStackEntry.arguments?.getString("isHost")?.toBooleanStrictOrNull() ?: false
            LobbyScreen(
                isHost = isHost,
                onNavigateBack = {
                    navController.popBackStack(Routes.HOME, false)
                },
                onGameStart = {
                    navController.navigate(Routes.ROLE_REVEAL) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.JOIN_ROOM) {
            JoinRoomScreen(
                onNavigateBack = { navController.popBackStack() },
                onRoomJoined = {
                    navController.navigate(Routes.lobby(false)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.ROLE_REVEAL) {
            RoleRevealScreen(
                onNavigateToGame = {
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.GAME) {
            GameScreen(
                onNavigateToVoting = {
                    navController.navigate(Routes.VOTING) {
                        popUpTo(Routes.GAME)
                    }
                },
                onNavigateToResult = {
                    navController.navigate(Routes.ROUND_RESULT) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.VOTING) {
            VotingScreen(
                onNavigateToResult = {
                    navController.navigate(Routes.ROUND_RESULT) {
                        popUpTo(Routes.HOME)
                    }
                },
                onNavigateBackToGame = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ROUND_RESULT) {
            RoundResultScreen(
                onNextRound = {
                    navController.navigate(Routes.ROLE_REVEAL) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBackToLobby = {
                    navController.navigate(Routes.lobby(true)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onEndGame = {
                    navController.navigate(Routes.SCOREBOARD) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.SCOREBOARD) {
            ScoreboardScreen(
                onPlayAgain = {
                    navController.navigate(Routes.lobby(true)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onNewGame = {
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }

        composable(Routes.HOW_TO_PLAY) {
            HowToPlayScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        navigation(
            startDestination = Routes.OFFLINE_SETUP,
            route = Routes.OFFLINE_GRAPH
        ) {
            composable(Routes.OFFLINE_SETUP) { entry ->
                val vm = entry.offlineViewModel(navController)
                OfflineSetupScreen(
                    viewModel = vm,
                    onNavigateBack = {
                        vm.reset()
                        navController.popBackStack(Routes.HOME, false)
                    },
                    onStart = { navController.navigate(Routes.OFFLINE_REVEAL) }
                )
            }

            composable(Routes.OFFLINE_REVEAL) { entry ->
                val vm = entry.offlineViewModel(navController)
                OfflineRevealScreen(
                    viewModel = vm,
                    onAllRevealed = {
                        navController.navigate(Routes.OFFLINE_GAME) {
                            popUpTo(Routes.OFFLINE_SETUP)
                        }
                    }
                )
            }

            composable(Routes.OFFLINE_GAME) { entry ->
                val vm = entry.offlineViewModel(navController)
                OfflineGameScreen(
                    viewModel = vm,
                    onRoundEnded = {
                        navController.navigate(Routes.OFFLINE_RESULT) {
                            popUpTo(Routes.OFFLINE_SETUP)
                        }
                    }
                )
            }

            composable(Routes.OFFLINE_RESULT) { entry ->
                val vm = entry.offlineViewModel(navController)
                OfflineResultScreen(
                    viewModel = vm,
                    onPlayAgain = {
                        navController.navigate(Routes.OFFLINE_REVEAL) {
                            popUpTo(Routes.OFFLINE_SETUP)
                        }
                    },
                    onBackHome = {
                        navController.popBackStack(Routes.HOME, false)
                    }
                )
            }
        }
    }
}

@Composable
private fun NavBackStackEntry.offlineViewModel(navController: NavController): OfflineModeViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry(Routes.OFFLINE_GRAPH)
    }
    return hiltViewModel(parentEntry)
}
