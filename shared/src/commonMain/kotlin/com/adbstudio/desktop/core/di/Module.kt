package com.adbstudio.desktop.core.di

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.commander.CommanderRegistry
import com.adbstudio.desktop.core.events.EventBus
import com.adbstudio.desktop.core.events.SimpleEventBus
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.apps.presentation.AppsViewModel
import com.adbstudio.desktop.feature.battery.presentation.BatteryViewModel
import com.adbstudio.desktop.feature.calendar.presentation.CalendarViewModel
import com.adbstudio.desktop.feature.contacts.presentation.ContactsViewModel
import com.adbstudio.desktop.feature.lifecycle.presentation.LifecycleViewModel
import com.adbstudio.desktop.feature.media.data.MediaRepository
import com.adbstudio.desktop.feature.media.presentation.MediaViewModel
import com.adbstudio.desktop.feature.settings.presentation.SettingsViewModel
import org.koin.dsl.module

val coreModule = module {
    single<EventBus> { SimpleEventBus() }
    single { CommanderRegistry() }
}

val adbModule = module {
    single { AdbManager() }
}

val deviceModule = module {
    single { DeviceRepository(get(), get()) }
}

val appsModule = module {
    single { AppsViewModel(get(), get()) }
}

val batteryModule = module {
    single { BatteryViewModel(get(), get()) }
}

val settingsModule = module {
    single { SettingsViewModel(get(), get()) }
}

val calendarModule = module {
    single { CalendarViewModel(get(), get()) }
}

val contactsModule = module {
    single { ContactsViewModel(get(), get()) }
}

val appModules = listOf(
    coreModule,
    adbModule,
    deviceModule,
    appsModule,
    batteryModule,
    settingsModule,
    calendarModule,
    contactsModule,
)
