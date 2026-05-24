package uz.angrykitten.spygame.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.angrykitten.spygame.data.PreferencesRepository
import uz.angrykitten.spygame.data.SessionRepository
import uz.angrykitten.spygame.data.WordPackRepository
import uz.angrykitten.spygame.network.HostSocketManager
import uz.angrykitten.spygame.network.NetworkRepository
import uz.angrykitten.spygame.network.NsdHelper
import uz.angrykitten.spygame.network.PeerSocketManager
import uz.angrykitten.spygame.sound.SoundManager
import uz.angrykitten.spygame.util.QRGenerator
import uz.angrykitten.spygame.util.QRScanner
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesRepository(
        @ApplicationContext context: Context
    ): PreferencesRepository = PreferencesRepository(context)

    @Provides
    @Singleton
    fun provideWordPackRepository(): WordPackRepository = WordPackRepository()

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = SessionRepository()

    @Provides
    @Singleton
    fun provideNsdHelper(
        @ApplicationContext context: Context
    ): NsdHelper = NsdHelper(context)

    @Provides
    @Singleton
    fun provideHostSocketManager(): HostSocketManager = HostSocketManager()

    @Provides
    @Singleton
    fun providePeerSocketManager(): PeerSocketManager = PeerSocketManager()

    @Provides
    @Singleton
    fun provideNetworkRepository(
        @ApplicationContext context: Context,
        nsdHelper: NsdHelper,
        hostSocketManager: HostSocketManager,
        peerSocketManager: PeerSocketManager
    ): NetworkRepository = NetworkRepository(context, nsdHelper, hostSocketManager, peerSocketManager)

    @Provides
    @Singleton
    fun provideSoundManager(
        @ApplicationContext context: Context,
        preferencesRepository: PreferencesRepository
    ): SoundManager = SoundManager(context, preferencesRepository)

    @Provides
    @Singleton
    fun provideQRGenerator(): QRGenerator = QRGenerator()

    @Provides
    @Singleton
    fun provideQRScanner(): QRScanner = QRScanner()
}
