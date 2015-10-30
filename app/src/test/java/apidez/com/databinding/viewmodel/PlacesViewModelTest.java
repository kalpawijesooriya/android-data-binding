package apidez.com.databinding.viewmodel;

import android.test.suitebuilder.annotation.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import apidez.com.databinding.model.api.IPlacesApi;
import apidez.com.databinding.model.entity.GoogleSearchResult;
import apidez.com.databinding.model.entity.Place;
import apidez.com.databinding.utils.TestDataUtils;
import rx.Observable;
import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by nongdenchet on 10/30/15.
 */
@MediumTest
@RunWith(JUnit4.class)
public class PlacesViewModelTest {
    private PlacesViewModel placesViewModel;
    private IPlacesApi placesApi;
    private TestSubscriber<Boolean> testSubscriber;
    private TestSubscriber<List<Place>> testSubscriberPlaces;

    @Before
    public void setUpViewModel() {
        placesApi = Mockito.mock(IPlacesApi.class);
        placesViewModel = new PlacesViewModel(placesApi);
        testSubscriber = TestSubscriber.create();
        testSubscriberPlaces = TestSubscriber.create();
        when(placesApi.placesResult()).thenReturn(testDataObservable());
    }

    @Test
    public void fetchAllPlacesSuccess() {
        placesViewModel.fetchAllPlaces().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    public void fetchAllPlaces() {
        placesViewModel.fetchAllPlaces().subscribe();
        assertEquals(placesViewModel.getCurrentPlaces().size(), 10);
    }

    @Test
    public void filterAll() {
        assertEquals(getAndFilterWith("all").size(), 10);
        getAndFilterWith("cafe");
        assertEquals(getAndFilterWith("all").size(), 10);
    }

    @Test
    public void filterFood() {
        assertEquals(getAndFilterWith("food").size(), 4);
        getAndFilterWith("cafe");
        assertEquals(getAndFilterWith("food").size(), 4);
    }

    @Test
    public void filterCafe() {
        assertEquals(getAndFilterWith("cafe").size(), 5);
    }

    @Test
    public void filterStore() {
        assertEquals(getAndFilterWith("store").size(), 4);
    }

    @Test
    public void filterRestaurant() {
        assertEquals(getAndFilterWith("restaurant").size(), 3);
        getAndFilterWith("cafe");
        assertEquals(getAndFilterWith("restaurant").size(), 3);
    }

    @Test
    public void filterTheater() {
        assertEquals(getAndFilterWith("theater").size(), 3);
    }

    @Test
    public void fetchTimeout() throws Exception {
        when(placesApi.placesResult()).thenReturn(
                Observable.create(subscriber -> {
                    try {
                        Thread.sleep(5100);
                        subscriber.onNext(TestDataUtils.nearByData());
                        subscriber.onCompleted();
                    } catch (InterruptedException e) {
                        subscriber.onError(e);
                    }
                })
        );
        try {
            boolean success = placesViewModel.fetchAllPlaces().toBlocking().first();
            if (success) fail("Should be timeout");
        } catch (Exception ignored) {
            // The test pass here, it should be timeout
        }
    }

    @Test
    public void fetchOnTime() throws Exception {
        when(placesApi.placesResult()).thenReturn(
                Observable.create(subscriber -> {
                    try {
                        Thread.sleep(4900);
                        subscriber.onNext(TestDataUtils.nearByData());
                        subscriber.onCompleted();
                    } catch (InterruptedException e) {
                        subscriber.onError(e);
                    }
                })
        );
        try {
            boolean success = placesViewModel.fetchAllPlaces().toBlocking().first();
            assertTrue(success);
        } catch (Exception ignored) {
            fail("Should be on time");
        }
    }

    @Test
    public void fetchRetry() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        when(placesApi.placesResult()).thenReturn(
                Observable.create(subscriber -> {
                    if (atomicInteger.getAndIncrement() < 3) {
                        subscriber.onError(new Exception());
                    } else {
                        subscriber.onNext(TestDataUtils.nearByData());
                        subscriber.onCompleted();
                    }
                })
        );
        try {
            boolean success = placesViewModel.fetchAllPlaces().toBlocking().first();
            verify(placesApi).placesResult();
            assertTrue(success);
        } catch (Exception ignored) {
            fail("Have to retry three times");
        }
    }

    @Test
    public void fetchExceedRetry() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        when(placesApi.placesResult()).thenReturn(
                Observable.create(subscriber -> {
                    if (atomicInteger.getAndIncrement() < 4) {
                        subscriber.onError(new Exception());
                    } else {
                        subscriber.onNext(TestDataUtils.nearByData());
                        subscriber.onCompleted();
                    }
                })
        );
        try {
            placesViewModel.fetchAllPlaces().toBlocking().first();
            fail("Should be out of retry");
        } catch (Exception ignored) {
            // Test pass here
        }
    }

    private List<Place> getAndFilterWith(String type) {
        placesViewModel.fetchAllPlaces().subscribe();
        placesViewModel.filterPlacesByType(type);
        return placesViewModel.getCurrentPlaces();
    }

    private Observable<GoogleSearchResult> testDataObservable() {
        return Observable.just(TestDataUtils.nearByData());
    }
}