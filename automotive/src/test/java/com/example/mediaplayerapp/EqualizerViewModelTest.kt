package com.example.mediaplayerapp.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4 // Use JUnit4 runner for pure JVM tests


/**
 * Unit tests for the EqualizerViewModel.
 */
@RunWith(JUnit4::class) // Specifies the test runner
class EqualizerViewModelTest {

    // Rule to make LiveData work on a background thread instantly
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // The ViewModel instance to test
    private lateinit var viewModel: EqualizerViewModel

    // Constants representing slider values (0-10 range assumed)
    private val SLIDER_MIN = 0
    private val SLIDER_MIDDLE = 5
    private val SLIDER_MAX = 10

    // Tolerance for floating point comparisons
    private val FLOAT_DELTA = 0.001f

    @Before
    fun setUp() {
        // Initialize the ViewModel before each test
        viewModel = EqualizerViewModel()
    }

    // --- Test Initial Values ---

    @Test
    fun initialBassValueIsMiddle() {
        // Assert that the initial value of bass LiveData is the middle value
        assertEquals(SLIDER_MIDDLE, viewModel.bass.value)
    }

    @Test
    fun initialMidValueIsMiddle() {
        // Assert that the initial value of mid LiveData is the middle value
        assertEquals(SLIDER_MIDDLE, viewModel.mid.value)
    }

    @Test
    fun initialTrebleValueIsMiddle() {
        // Assert that the initial value of treble LiveData is the middle value
        assertEquals(SLIDER_MIDDLE, viewModel.treble.value)
    }

    // --- Test setBass method ---

    @Test
    fun setBass_updatesValueWhenDifferent() {
        val newValue = SLIDER_MAX // Choose a value different from the initial (5)

        // Call the method
        viewModel.setBass(newValue)

        // Assert that the LiveData value has been updated
        assertEquals(newValue, viewModel.bass.value)
    }

    @Test
    fun setBass_doesNotUpdateValueWhenSame() {
        val initialValue = viewModel.bass.value // Get the current value

        // Call the method with the same value
        viewModel.setBass(initialValue!!) // Use non-null assertion as we set initial value

        // Assert that the LiveData value has NOT been updated (it should remain the same)
        assertEquals(initialValue, viewModel.bass.value)
    }

    // --- Test setMid method ---

    @Test
    fun setMid_updatesValueWhenDifferent() {
        val newValue = SLIDER_MIN

        viewModel.setMid(newValue)

        assertEquals(newValue, viewModel.mid.value)
    }

    @Test
    fun setMid_doesNotUpdateValueWhenSame() {
        val initialValue = viewModel.mid.value

        viewModel.setMid(initialValue!!)

        assertEquals(initialValue, viewModel.mid.value)
    }

    // --- Test setTreble method ---

    @Test
    fun setTreble_updatesValueWhenDifferent() {
        val newValue = SLIDER_MIDDLE + 2 // Example: 7

        viewModel.setTreble(newValue)

        assertEquals(newValue, viewModel.treble.value)
    }

    @Test
    fun setTreble_doesNotUpdateValueWhenSame() {
        val initialValue = viewModel.treble.value

        viewModel.setTreble(initialValue!!)

        assertEquals(initialValue, viewModel.treble.value)
    }

    // --- Test getBassDb method ---

    @Test
    fun getBassDb_returnsZeroForMiddleValue() {
        // Ensure bass is at the initial middle value (5)
        assertEquals(SLIDER_MIDDLE, viewModel.bass.value)

        // Calculate and assert the dB value
        assertEquals(0.0f, viewModel.getBassDb(), FLOAT_DELTA)
    }

    @Test
    fun getBassDb_returnsCorrectDbForMinValue() {
        // Set bass to the minimum slider value (0)
        viewModel.setBass(SLIDER_MIN)

        // Calculate the expected dB: (0 - 5) * 2.4f = -12.0f
        val expectedDb = (SLIDER_MIN - SLIDER_MIDDLE) * 2.4f

        // Assert the calculated dB value
        assertEquals(expectedDb, viewModel.getBassDb(), FLOAT_DELTA)
    }

    @Test
    fun getBassDb_returnsCorrectDbForMaxValue() {
        // Set bass to the maximum slider value (10)
        viewModel.setBass(SLIDER_MAX)

        // Calculate the expected dB: (10 - 5) * 2.4f = 12.0f
        val expectedDb = (SLIDER_MAX - SLIDER_MIDDLE) * 2.4f

        // Assert the calculated dB value
        assertEquals(expectedDb, viewModel.getBassDb(), FLOAT_DELTA)
    }

    @Test
    fun getBassDb_returnsCorrectDbForIntermediateValue() {
        val sliderValue = 2 // Example intermediate value

        // Set bass to an intermediate value
        viewModel.setBass(sliderValue)

        // Calculate the expected dB: (2 - 5) * 2.4f = -7.2f
        val expectedDb = (sliderValue - SLIDER_MIDDLE) * 2.4f

        // Assert the calculated dB value
        assertEquals(expectedDb, viewModel.getBassDb(), FLOAT_DELTA)
    }

    // --- Test getMidDb method ---

    @Test
    fun getMidDb_returnsZeroForMiddleValue() {
        assertEquals(SLIDER_MIDDLE, viewModel.mid.value)
        assertEquals(0.0f, viewModel.getMidDb(), FLOAT_DELTA)
    }

    @Test
    fun getMidDb_returnsCorrectDbForMinValue() {
        viewModel.setMid(SLIDER_MIN)
        val expectedDb = (SLIDER_MIN - SLIDER_MIDDLE) * 2.4f
        assertEquals(expectedDb, viewModel.getMidDb(), FLOAT_DELTA)
    }

    @Test
    fun getMidDb_returnsCorrectDbForMaxValue() {
        viewModel.setMid(SLIDER_MAX)
        val expectedDb = (SLIDER_MAX - SLIDER_MIDDLE) * 2.4f
        assertEquals(expectedDb, viewModel.getMidDb(), FLOAT_DELTA)
    }

    @Test
    fun getMidDb_returnsCorrectDbForIntermediateValue() {
        val sliderValue = 8
        viewModel.setMid(sliderValue)
        val expectedDb = (sliderValue - SLIDER_MIDDLE) * 2.4f
        assertEquals(expectedDb, viewModel.getMidDb(), FLOAT_DELTA)
    }

    // --- Test getTrebleDb method ---

    @Test
    fun getTrebleDb_returnsZeroForMiddleValue() {
        assertEquals(SLIDER_MIDDLE, viewModel.treble.value)
        assertEquals(0.0f, viewModel.getTrebleDb(), FLOAT_DELTA)
    }

    @Test
    fun getTrebleDb_returnsCorrectDbForMinValue() {
        viewModel.setTreble(SLIDER_MIN)
        val expectedDb = (SLIDER_MIN - SLIDER_MIDDLE) * 2.4f
        assertEquals(expectedDb, viewModel.getTrebleDb(), FLOAT_DELTA)
    }

    @Test
    fun getTrebleDb_returnsCorrectDbForMaxValue() {
        viewModel.setTreble(SLIDER_MAX)
        val expectedDb = (SLIDER_MAX - SLIDER_MIDDLE) * 2.4f
        assertEquals(expectedDb, viewModel.getTrebleDb(), FLOAT_DELTA)
    }

    @Test
    fun getTrebleDb_returnsCorrectDbForIntermediateValue() {
        val sliderValue = 3
        viewModel.setTreble(sliderValue)
        val expectedDb = (sliderValue - SLIDER_MIDDLE) * 2.4f
        assertEquals(expectedDb, viewModel.getTrebleDb(), FLOAT_DELTA)
    }
}
