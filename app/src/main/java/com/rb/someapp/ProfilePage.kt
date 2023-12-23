package com.rb.someapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.Animation
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.rb.someapp.databinding.ActivityProfilePageBinding

class ProfilePage : AppCompatActivity() {
    lateinit var binding: ActivityProfilePageBinding

    private lateinit var providers: ArrayList<AuthUI.IdpConfig>

    private var firebaseUser: FirebaseUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePageBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
        else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }

        providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseUser = FirebaseAuth.getInstance().currentUser

        handleProfileDetailsUi()

        val signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()){
            if (it.resultCode == RESULT_OK) {
                firebaseUser =  FirebaseAuth.getInstance().currentUser
                handleProfileDetailsUi()
                val snackbar = AppUtils.buildSnackbar(this, "Successfully signed in", binding.root, false)
                snackbar.duration = Snackbar.LENGTH_LONG
                snackbar.show()
            }
            else {
                val snackbar = AppUtils.buildSnackbar(this, "Unable to sign in, please try again later...", binding.root, false)
                snackbar.duration = Snackbar.LENGTH_LONG
                snackbar.show()
            }
        }

        val signInIntent = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).setIsSmartLockEnabled(false).setTheme(R.style.AuthTheme).build()


        binding.profileToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.profileSignInButton.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    signInLauncher.launch(signInIntent)

                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })
            it.startAnimation(anim)
        }

        binding.profileSignOutButton.setOnClickListener {
            AuthUI.getInstance().signOut(this).addOnCompleteListener {
                if (it.isSuccessful) {
                    //Update the firebase user and details
                    firebaseUser = null
                    handleProfileDetailsUi()
                    val snackbar = AppUtils.buildSnackbar(this, "Signed out successfully", binding.root, false)
                    snackbar.duration = Snackbar.LENGTH_LONG
                    snackbar.show()
                }
                else {
                    val snackbar = AppUtils.buildSnackbar(this, "Unable to sign out currently, try again later...", binding.root, false)
                    snackbar.duration = Snackbar.LENGTH_LONG
                    snackbar.show()
                }
            }

        }

        binding.profileOrdsCard.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    val intent = Intent(this@ProfilePage, OrdersActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })
            it.startAnimation(anim)
        }

        binding.profileFavCard.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    val intent = Intent(this@ProfilePage, FavouritesPage::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })
            it.startAnimation(anim)
        }

        binding.profileAddrsCard.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    val intent = Intent(this@ProfilePage, AddressActivity::class.java)
                    intent.putExtra("isSelection", false)
                    startActivity(intent)
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })

            it.startAnimation(anim)
        }

        binding.profileSettingsCard.setOnClickListener {
            val anim = AnimUtils.pressAnim(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    val intent = Intent(this@ProfilePage, SettingsPage::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_stable)
                }

                override fun onAnimationRepeat(p0: Animation?) {
                }

            })

            it.startAnimation(anim)
        }


    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_stable, R.anim.activity_close)

    }

    private fun handleProfileDetailsUi() {
        if (firebaseUser == null) {
            binding.profileDetailsCard.visibility = View.GONE
            binding.profileSignInButton.visibility = View.VISIBLE
            binding.signInText.visibility = View.VISIBLE
        }
        else {
            binding.profileDetailsCard.visibility = View.VISIBLE
            binding.profileSignInButton.visibility = View.GONE
            binding.signInText.visibility = View.GONE


            binding.profileName.text = firebaseUser!!.displayName
            binding.profileContact.text = firebaseUser!!.email
            Glide.with(this).asDrawable().load(firebaseUser!!.photoUrl).override(AppUtils.dptopx(this, 70)).centerCrop().circleCrop().into(binding.profileImg)
        }
    }


}