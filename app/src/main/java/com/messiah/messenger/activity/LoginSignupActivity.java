package com.messiah.messenger.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;
import com.messiah.messenger.R;
import com.messiah.messenger.helpers.XmppHelper;
import com.messiah.messenger.service.XmppService;
import com.messiah.messenger.utils.Utils;

import java.util.concurrent.TimeUnit;

import xdroid.toaster.Toaster;

/**
 * A login screen that offers login via email/password.
 */
public class LoginSignupActivity extends AppCompatActivity  {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private AsyncTask<Void, Void, Boolean> mAuthTask = null;

    // UI references.
    private CountryCodePicker mCcpView;
    private EditText mPhoneView;
    private View mLoginFormView;
    private View mContentView;
    private View mCodeConfirmationForm;
    private Button mEmailSignInButton;

    private View mProgressView;

    private String mPhoneNumber;

    private String mVerificationId;
    //    private boolean isNewAccount = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup);

        mCcpView = (CountryCodePicker) findViewById(R.id.ccp);
        mPhoneView = (AutoCompleteTextView) findViewById(R.id.phone);

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(view -> attemptLogin());

        mLoginFormView = findViewById(R.id.login_form);
        mContentView = findViewById(R.id.content);
        mCodeConfirmationForm = findViewById(R.id.code_confirmation_form);
        mProgressView = findViewById(R.id.login_progress);

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mPhoneView.setError(null);

        // Store values at the time of the login attempt.
        String phone = mPhoneView.getText().toString();
        String countryCode = mCcpView.getSelectedCountryCodeWithPlus();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
//        if (TextUtils.isEmpty(password) ) {
//            mPasswordView.setError(getString(R.string.error_field_required));
//            focusView = mPasswordView;
//            cancel = true;
//        } else if (!isPasswordValid(password)) {
//            mPasswordView.setError(getString(R.string.error_invalid_password));
//            focusView = mPasswordView;
//            cancel = true;
//        } else if (isNewAccount && !password.equals(repeatedPassword)){
//            mRepeatPasswordView.setError(getString(R.string.error_passwords_dont_match));
//            mPasswordView.setError(getString(R.string.error_passwords_dont_match));
//            focusView = mPasswordView;
//            cancel = true;
//        }

//        if (isNewAccount){
//            // Check for a valid email address.
//            if (TextUtils.isEmpty(email)) {
//                mEmailView.setError(getString(R.string.error_field_required));
//                focusView = mEmailView;
//                cancel = true;
//            } else if (! isEmailValid(email)) {
//                mEmailView.setError(getString(R.string.error_invalid_email));
//                focusView = mEmailView;
//                cancel = true;
//            }
//        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(phone) ) {
            mPhoneView.setError(getString(R.string.error_field_required));
            focusView = mPhoneView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            mPhoneNumber = countryCode + phone;

            PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    Log.d("firebaseAuth", "onVerificationCompleted:" + credential);
                    signInWithPhoneAuthCredential(credential);
                    showProgress(true);
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    // This callback is invoked in an invalid request for verification is made,
                    // for instance if the the phone number format is not valid.
                    Log.w("firebaseAuth", "onVerificationFailed", e);

                    Toaster.toast("An error occurred: " +  e.getMessage());
                    showForm(false);
                }

                @Override
                public void onCodeSent(String verificationId,
                                       PhoneAuthProvider.ForceResendingToken token) {
                    Log.d("firebaseAuth", "onCodeSent:" + verificationId + " " + token.toString());
                    mVerificationId = verificationId;

                    showForm(true);
                }
            };

            Log.d("firebaseAuth",  countryCode + phone);
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    mPhoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    mCallbacks);        // OnVerificationStateChangedCallbacks


            showProgress(true);

        }
    }

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mContentView.setVisibility(show ? View.GONE : View.VISIBLE);
        mContentView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mContentView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void undoSendCode(View view) {
        showForm(false);
    }

    private void showForm(boolean isConfirmationForm){
        showProgress(false);
        mLoginFormView.setVisibility(isConfirmationForm ? View.GONE : View.VISIBLE);
        mCodeConfirmationForm.setVisibility(isConfirmationForm ? View.VISIBLE :View.GONE);

        ((TextView) findViewById(R.id.code_was_sent)).setText("Code was sent to " + mPhoneNumber + ", please enter it below when it arrives");

        mEmailSignInButton.setOnClickListener(isConfirmationForm ? (view) -> {
            String code = ((EditText) findViewById(R.id.code)).getText().toString();
            signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(mVerificationId, code));
        } : (view) -> attemptLogin());
        mEmailSignInButton.setText(isConfirmationForm ? "Confirm" : "Send code");
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("firebaseAuth", "signInWithCredential:success");
                        Utils.putPhoneNumber(this, mPhoneNumber);
                        startService(new Intent(LoginSignupActivity.this, XmppService.class));
                        startActivity(new Intent(LoginSignupActivity.this, MainActivity.class));

                    } else {
                        Log.w("firebaseAuth", "signInWithCredential:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {

                            Toaster.toast("An error occurred: wrong code!");
                        } else {

                            Toaster.toast("An error occurred: " + task.getException().getMessage());
                        }

                        showForm(true);
                    }
                });
    }
}

