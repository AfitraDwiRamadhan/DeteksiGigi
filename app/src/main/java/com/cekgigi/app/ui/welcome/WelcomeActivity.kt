package com.cekgigi.app.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.cekgigi.app.databinding.ActivityWelcomeBinding
import com.cekgigi.app.ui.dashboard.DashboardActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimations()

        binding.btnMulaiSkrining.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupAnimations() {
        // Logo entrance
        val logoFade = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            fillAfter = true
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val logoSlide = TranslateAnimation(0f, 0f, -50f, 0f).apply {
            duration = 1000
            fillAfter = true
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        binding.imgLogo.parent.let { (it as? android.view.View)?.startAnimation(logoFade) }
        binding.imgLogo.parent.let { (it as? android.view.View)?.startAnimation(logoSlide) }

        // Title and Subtitle
        val textFade = AlphaAnimation(0f, 1f).apply {
            duration = 800
            startOffset = 500
            fillAfter = true
        }
        binding.txtTitle.startAnimation(textFade)
        binding.txtSubtitle.startAnimation(textFade)

        // Button slide up
        val btnSlide = TranslateAnimation(0f, 0f, 100f, 0f).apply {
            duration = 800
            startOffset = 800
            fillAfter = true
            interpolator = AccelerateDecelerateInterpolator()
        }
        val btnFade = AlphaAnimation(0f, 1f).apply {
            duration = 800
            startOffset = 800
            fillAfter = true
        }
        
        binding.btnMulaiSkrining.startAnimation(btnSlide)
        binding.btnMulaiSkrining.startAnimation(btnFade)
    }
}
