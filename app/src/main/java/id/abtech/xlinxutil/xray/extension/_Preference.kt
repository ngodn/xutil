package id.abtech.xlinxutil.xray.extension

import android.preference.Preference

fun Preference.onClick(listener: () -> Unit) {
    setOnPreferenceClickListener {
        listener()
        true
    }
}