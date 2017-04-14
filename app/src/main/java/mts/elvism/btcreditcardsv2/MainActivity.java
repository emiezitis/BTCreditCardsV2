package mts.elvism.btcreditcardsv2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.cardform.OnCardFormSubmitListener;
import com.braintreepayments.cardform.utils.CardType;
import com.braintreepayments.cardform.view.CardEditText;
import com.braintreepayments.cardform.view.CardForm;
import com.braintreepayments.cardform.view.SupportedCardTypesView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements OnCardFormSubmitListener, CardEditText.OnCardTypeChangedListener, PaymentMethodNonceCreatedListener {

    private static final CardType[] SUPPORTED_CARD_TYPES = { CardType.VISA, CardType.MASTERCARD, CardType.DISCOVER,
            CardType.AMEX, CardType.DINERS_CLUB, CardType.JCB, CardType.MAESTRO, CardType.UNIONPAY };

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SERVER_BASE = "your_server_client_token_URL";
    private static final String CHECKOUT_BASE = "your_server_checkout_URL";

    protected CardForm mCardForm;
    private SupportedCardTypesView mSupportedCardTypesView;
    private BraintreeFragment mBraintreeFragment;
    private String mAuthorization;
    private CardForm cardForm;


    private AsyncHttpClient client = new AsyncHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getToken();

        mSupportedCardTypesView = (SupportedCardTypesView) findViewById(R.id.supported_card_types);
        mSupportedCardTypesView.setSupportedCardTypes(SUPPORTED_CARD_TYPES);

        mCardForm = (CardForm) findViewById(R.id.card_form);
        mCardForm.cardRequired(true)
                .expirationRequired(true)
                .cvvRequired(true)
                .actionLabel(getString(R.string.purchase))
                .setup(this);

        mCardForm.setOnCardFormSubmitListener(this);
        mCardForm.setOnCardTypeChangedListener(this);



    }



    @Override
    public void onCardFormSubmit() {
        if (mCardForm.isValid()) {

            Toast.makeText(this, R.string.valid, Toast.LENGTH_SHORT).show();

            try {
                mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
                // mBraintreeFragment is ready to use!
            } catch (InvalidArgumentException e) {
                // There was an issue with your authorization string.
            }

            CardBuilder cardBuilder = new CardBuilder()
                    .cardNumber(mCardForm.getCardNumber())
                    .expirationMonth(mCardForm.getExpirationMonth())
                    .expirationYear(mCardForm.getExpirationYear())
                    .cvv(mCardForm.getCvv());

            Card.tokenize(mBraintreeFragment, cardBuilder);

        } else {
            mCardForm.validate();
            Toast.makeText(this, R.string.invalid, Toast.LENGTH_SHORT).show();

        }
    }


    @Override
    public void onCardTypeChanged(CardType cardType) {
        if (cardType == CardType.EMPTY) {
            mSupportedCardTypesView.setSupportedCardTypes(SUPPORTED_CARD_TYPES);
        } else {
            mSupportedCardTypesView.setSelected(cardType);
        }

    }

    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        // Send nonce to server
        String nonce = paymentMethodNonce.getNonce();
            postNonceToServer(nonce);

    }

    private void postNonceToServer(String nonce) {

        RequestParams params = new RequestParams();
        params.put("payment_method_nonce", nonce);
        params.put("amount","10.00");

        client.post(CHECKOUT_BASE + "/payment", params,
                new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Toast.makeText(MainActivity.this, responseString, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Error: " + responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {

                        Toast.makeText(MainActivity.this, responseString, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Success: " + responseString);

                    }
                    // Your implementation here
                }
        );
    }

    private void getToken(){


        client.get(SERVER_BASE + "/token", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                mAuthorization = responseString;

            }
        });

    }

}
