package com.posturedetection.android

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.posturedetection.android.camera.CameraSource
import com.posturedetection.android.data.Camera
import com.posturedetection.android.data.Device
import com.posturedetection.android.data.model.AccountSettings
import com.posturedetection.android.databinding.ActivityHomeBinding
import com.posturedetection.android.ml.ModelType
import com.posturedetection.android.ml.MoveNet
import com.posturedetection.android.ml.PoseClassifier
import com.posturedetection.android.ui.profile.ProfileFragment
import com.posturedetection.android.ui.statistics.StatisticsFragment
import com.posturedetection.android.ui.statistics.StatisticsViewModel
import com.posturedetection.android.util.ActivityCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import reset

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var statisticsViewModel: StatisticsViewModel
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** 为视频画面创建一个 SurfaceView */
    private lateinit var surfaceView: SurfaceView

    /** 修改默认计算设备：CPU、GPU、NNAPI（AI加速器） */
    private var device = Device.CPU
    /** 修改默认摄像头：FRONT、BACK */
    private var selectedCamera = Camera.BACK

    /** 定义几个计数器 */
    private var forwardheadCounter = 0
    private var crosslegCounter = 0
    private var standardCounter = 0
    private var missingCounter = 0



    /** 定义一个历史姿态寄存器 */
    private var poseRegister = "standard"

    /** 设置一个用来显示 Debug 信息的 TextView */
    private lateinit var tvDebug: TextView

    /** 设置一个用来显示当前坐姿状态的 ImageView */
    private lateinit var ivStatus: ImageView

    private lateinit var tvFPS: TextView
    private lateinit var tvScore: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnCamera: Spinner

    private var cameraSource: CameraSource? = null
    private var isClassifyPose = true

    private val  requestPermissionLauncher=
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                /** 得到用户相机授权后，程序开始运行 */
                openCamera()
            } else {
                /** 提示用户“未获得相机权限，应用无法运行” */
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            /** 如果用户未选择运算设备，使用默认设备进行计算 */
        }
    }

    private var changeCameraListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, view: View?, direction: Int, id: Long) {
            changeCamera(direction)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            /** 如果用户未选择摄像头，使用默认摄像头进行拍摄 */
        }
    }


    fun changeBySp(){
        //get sp
        val sp = getSharedPreferences("account_settings", MODE_PRIVATE)
        var account_settings_json = sp.getString("account_settings", null)
        if (account_settings_json != null){
            var gson = Gson()
            var accountSettings = gson.fromJson(account_settings_json, AccountSettings::class.java)
            if (accountSettings != null){
                val targetDevice = when (accountSettings.aiDevice) {
                    0 -> Device.CPU
                    1 -> Device.GPU
                    else -> Device.NNAPI
                }
                if (device == targetDevice) return
                device = targetDevice


                val targetCamera = when (accountSettings.camera) {
                    0 -> Camera.BACK
                    else -> Camera.FRONT
                }
                if (selectedCamera == targetCamera) return
                selectedCamera = targetCamera

                createPoseEstimator()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCollector.addActivity(this)

        statisticsViewModel = ViewModelProvider(this).get(StatisticsViewModel::class.java)


        val navView: BottomNavigationView = binding.navView
        navView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_detection -> {
                    // Start the Home activity

                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.navigation_statistics -> {
                    cameraSource?.close()
                    cameraSource = null
                    StatisticsDataUtils.writeCounterToSharedPreferences(this, statisticsViewModel.counterData)
                    statisticsViewModel.counterData.reset()
                    showFragment(StatisticsFragment())
                    //statisticsViewModel.counterData.reset()
                    true
                }
                R.id.navigation_profile -> {
                    cameraSource?.close()
                    cameraSource = null
                    showFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)
        /** 用来显示 Debug 信息 */
        tvDebug = findViewById(R.id.tvDebug)
        /** 用来显示当前坐姿状态 */
        ivStatus = findViewById(R.id.ivStatus)

        tvFPS = findViewById(R.id.tvFps)
        spnDevice = findViewById(R.id.spnDevice)
        spnCamera = findViewById(R.id.spnCamera)
        surfaceView = findViewById(R.id.surfaceView)

        initSpinner()
        if (!isCameraPermissionGranted()) {
            changeBySp()
            requestPermission()
        }
    }

    override fun onStart() {
        changeBySp()
        openCamera()
        super.onStart()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
//        cameraSource?.close()
//        cameraSource = null
    }

    /** 检查相机权限是否有授权 */
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCamera() {
        /** 音频播放 */
        val crosslegPlayer = MediaPlayer.create(this, R.raw.crossleg)
        val forwardheadPlayer = MediaPlayer.create(this, R.raw.forwardhead)
        val standardPlayer = MediaPlayer.create(this, R.raw.standard)
        var crosslegPlayerFlag = true
        var forwardheadPlayerFlag = true
        var standardPlayerFlag = true

        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, selectedCamera, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {

                            /** 解释一下，tfe_pe_tv 的意思：tensorflow example、pose estimation、text view */
                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        /** 对检测结果进行处理 */
                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?
                        ) {
                            tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)

                            /** 分析目标姿态，给出提示 */
                            if (poseLabels != null && personScore != null && personScore > 0.3) {
                                missingCounter = 0
                                val sortedLabels = poseLabels.sortedByDescending { it.second }
                                when (sortedLabels[0].first) {
                                    "forwardhead" -> {
                                        crosslegCounter = 0
                                        standardCounter = 0
                                        if (poseRegister == "forwardhead") {
                                            forwardheadCounter++
                                            statisticsViewModel.counterData.forwardheadCounter++
                                        }
                                        poseRegister = "forwardhead"

                                        /** 显示当前坐姿状态：脖子前伸 */
                                        if (forwardheadCounter > 60) {

                                            /** 播放提示音 */
                                            if (forwardheadPlayerFlag) {
                                                forwardheadPlayer.start()
                                            }
                                            standardPlayerFlag = true
                                            crosslegPlayerFlag = true
                                            forwardheadPlayerFlag = false
                                            runOnUiThread {
                                                ivStatus.setImageResource(R.drawable.forwardhead_confirm)
                                            }
                                        }

                                        /** 显示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $forwardheadCounter")
                                    }
                                    "crossleg" -> {
                                        forwardheadCounter = 0
                                        standardCounter = 0
                                        if (poseRegister == "crossleg") {
                                            crosslegCounter++
                                            statisticsViewModel.counterData.crosslegCounter++
                                        }
                                        poseRegister = "crossleg"

                                        /** 显示当前坐姿状态：翘二郎腿 */
                                        if (crosslegCounter > 60) {

                                            /** 播放提示音 */
                                            if (crosslegPlayerFlag) {
                                                crosslegPlayer.start()
                                            }
                                            standardPlayerFlag = true
                                            crosslegPlayerFlag = false
                                            forwardheadPlayerFlag = true
                                            runOnUiThread {
                                                ivStatus.setImageResource(R.drawable.crossleg_confirm)
                                            }
                                        }
                                        /** 显示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $crosslegCounter")
                                    }
                                    else -> {
                                        forwardheadCounter = 0
                                        crosslegCounter = 0
                                        if (poseRegister == "standard") {
                                            standardCounter++
                                            statisticsViewModel.counterData.standardCounter++
                                        }
                                        poseRegister = "standard"

                                        /** 显示当前坐姿状态：标准 */
                                        if (standardCounter > 30) {

                                            /** 播放提示音：坐姿标准 */
                                            if (standardPlayerFlag) {
                                                standardPlayer.start()
                                            }
                                            standardPlayerFlag = false
                                            crosslegPlayerFlag = true
                                            forwardheadPlayerFlag = true
                                            runOnUiThread {
                                                ivStatus.setImageResource(R.drawable.standard)
                                            }
                                        }

                                        /** 显示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $standardCounter")
                                    }
                                }


                            }
                            else {
                                missingCounter++
                                if (missingCounter > 30) {
                                    runOnUiThread {
                                        ivStatus.setImageResource(R.drawable.no_target)
                                    }
                                }

                                /** 显示 Debug 信息 */
                                tvDebug.text = getString(R.string.tfe_pe_tv_debug, "missing $missingCounter")
                            }
                        }
                    }).apply {
                        prepareCamera()
                    }
                isPoseClassifier()
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }

    private fun isPoseClassifier() {
        cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(this) else null)
    }

    /** 初始化运算设备选项菜单（CPU、GPU、NNAPI） */
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adapter
            spnDevice.onItemSelectedListener = changeDeviceListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_camera_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnCamera.adapter = adapter
            spnCamera.onItemSelectedListener = changeCameraListener
        }
    }

    /** 在程序运行过程中切换运算设备 */
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    /** 在程序运行过程中切换摄像头 */
    private fun changeCamera(direaction: Int) {
        val targetCamera = when (direaction) {
            0 -> Camera.BACK
            else -> Camera.FRONT
        }
        if (selectedCamera == targetCamera) return
        selectedCamera = targetCamera

        cameraSource?.close()
        cameraSource = null
        openCamera()
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(this, device, ModelType.Thunder)
        poseDetector.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    /** 显示报错信息 */
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // pass
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }




}