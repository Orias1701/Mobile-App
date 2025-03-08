@file:Suppress("DEPRECATION")

package com.example.batterysavior

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var batteryStatusTextView: TextView
    private lateinit var batteryTimeTextView: TextView
    private lateinit var chargeTimeTextView: TextView
    private lateinit var batteryWarningLevelSeekBar: SeekBar
    private lateinit var batteryWarningLevelText: TextView
    private lateinit var selectSoundButton: Button
    private lateinit var optimizeModeSpinner: Spinner
    private var lowBatteryLevel = 20
    private var selectedSoundUri: Uri? = null
    private lateinit var ringtonePickerLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        batteryStatusTextView = findViewById(R.id.battery_status)
        batteryTimeTextView = findViewById(R.id.battery_time)
        chargeTimeTextView = findViewById(R.id.charge_time)
        batteryWarningLevelSeekBar = findViewById(R.id.battery_warning_seekbar)
        batteryWarningLevelText = findViewById(R.id.battery_warning_text)
        selectSoundButton = findViewById(R.id.select_sound_button)
        optimizeModeSpinner = findViewById(R.id.optimize_mode_spinner)

        val modes = arrayOf("Mặc định", "Chơi game", "Xem phim", "Đọc báo", "Lướt web", "Tiết kiệm tối đa")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        optimizeModeSpinner.adapter = adapter

        optimizeModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                Toast.makeText(this@MainActivity, "Chế độ: ${modes[position]}", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        batteryWarningLevelSeekBar.progress = lowBatteryLevel
        batteryWarningLevelText.text = "Ngưỡng cảnh báo: $lowBatteryLevel%"

        batteryWarningLevelSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lowBatteryLevel = progress
                batteryWarningLevelText.text = "Ngưỡng cảnh báo: $lowBatteryLevel%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedSoundUri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                Toast.makeText(this, "Đã chọn âm báo mới", Toast.LENGTH_SHORT).show()
            }
        }

        selectSoundButton.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn âm báo pin yếu")
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            ringtonePickerLauncher.launch(intent)
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = level * 100 / scale.toFloat()
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

                val estimatedTime = if (currentNow != 0) {
                    (chargeCounter / abs(currentNow.toFloat()) * 60).toInt()
                } else {
                    -1
                }

                val chargeTime = if (isCharging && currentNow > 0) {
                    val remainingCapacity = (scale - level) * chargeCounter / scale
                    (remainingCapacity / currentNow.toFloat() * 60).toInt()
                } else {
                    -1
                }

                val timeText = if (estimatedTime > 0) "Ước tính thời gian sử dụng còn lại: $estimatedTime phút" else "Không thể ước tính thời gian sử dụng"
                val chargeTimeText = if (chargeTime > 0) "Ước tính thời gian sạc đầy: $chargeTime phút" else "Không thể ước tính thời gian sạc đầy"
                val statusText = "Mức pin: $batteryPct%\nTrạng thái sạc: ${if (isCharging) "Đang sạc" else "Không sạc"}"

                batteryStatusTextView.text = statusText
                batteryTimeTextView.text = timeText
                chargeTimeTextView.text = chargeTimeText

                if (batteryPct <= lowBatteryLevel && !isCharging) {
                    triggerLowBatteryAlert()
                }
            }
        }
    }

    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    private fun triggerLowBatteryAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }

        val ringtone = selectedSoundUri?.let { RingtoneManager.getRingtone(applicationContext, it) }
            ?: RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        ringtone?.play()
        Toast.makeText(this, "Cảnh báo: Pin yếu!", Toast.LENGTH_LONG).show()
    }
}