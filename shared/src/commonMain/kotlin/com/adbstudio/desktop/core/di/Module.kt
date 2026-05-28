package com.adbstudio.desktop.core.di

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.base.CommandRegistry
import com.adbstudio.desktop.commander.CommanderRegistry
import com.adbstudio.desktop.core.events.EventBus
import com.adbstudio.desktop.core.events.SimpleEventBus
import com.adbstudio.desktop.core.task.SimpleTaskManager
import com.adbstudio.desktop.core.task.TaskManager
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.apps.presentation.AppsViewModel
import com.adbstudio.desktop.feature.battery.presentation.BatteryViewModel
import com.adbstudio.desktop.feature.calendar.presentation.CalendarViewModel
import com.adbstudio.desktop.feature.contacts.presentation.ContactsViewModel
import com.adbstudio.desktop.feature.inspector.presentation.UiInspectorViewModel
import com.adbstudio.desktop.feature.lifecycle.presentation.LifecycleViewModel
import com.adbstudio.desktop.feature.media.data.MediaRepository
import com.adbstudio.desktop.feature.media.presentation.MediaViewModel
import com.adbstudio.desktop.feature.messages.presentation.MessagesViewModel
import com.adbstudio.desktop.feature.notification.presentation.NotificationViewModel
import com.adbstudio.desktop.feature.settings.presentation.SettingsViewModel
import org.koin.dsl.module

val coreModule = module {
    single<EventBus> { SimpleEventBus() }
    single { CommanderRegistry() }
    single<TaskManager> { SimpleTaskManager() }
}

val adbModule = module {
    single { AdbManager() }
    single { CommandRegistry() }
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

val lifecycleModule = module {
    single { LifecycleViewModel(get(), get()) }
}

val mediaModule = module {
    single { MediaRepository(get()) }
    single { MediaViewModel(get(), get()) }
}

val messagesModule = module {
    single { MessagesViewModel(get(), get()) }
}

val notificationModule = module {
    single { NotificationViewModel(get(), get()) }
}

val uiInspectorModule = module {
    single { UiInspectorViewModel(get(), get()) }
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
    lifecycleModule,
    mediaModule,
    messagesModule,
    notificationModule,
    uiInspectorModule,
)
