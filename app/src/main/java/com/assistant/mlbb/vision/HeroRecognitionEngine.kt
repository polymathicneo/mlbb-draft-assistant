package com.assistant.mlbb.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.assistant.mlbb.services.OverlayService
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class HeroRecognitionEngine(private val context: Context) {

    private var interpreter: Interpreter? = null
    
    // Normalized coordinates for Draft Screen slots
    // These should rotate/scale based on aspect ratio
    data class Slot(val name: String, val rect: RectF)

    private val pickSlotsLeft = listOf(
        Slot("L1", RectF(0.02f, 0.20f, 0.12f, 0.32f)),
        Slot("L2", RectF(0.02f, 0.33f, 0.12f, 0.45f)),
        Slot("L3", RectF(0.02f, 0.46f, 0.12f, 0.58f)),
        Slot("L4", RectF(0.02f, 0.59f, 0.12f, 0.71f)),
        Slot("L5", RectF(0.02f, 0.72f, 0.12f, 0.84f))
    )

    private val pickSlotsRight = listOf(
        Slot("R1", RectF(0.88f, 0.20f, 0.98f, 0.32f)),
        Slot("R2", RectF(0.88f, 0.33f, 0.98f, 0.45f)),
        Slot("R3", RectF(0.88f, 0.46f, 0.98f, 0.58f)),
        Slot("R4", RectF(0.88f, 0.59f, 0.98f, 0.71f)),
        Slot("R5", RectF(0.88f, 0.72f, 0.98f, 0.84f))
    )

    init {
        try {
            // interpreter = Interpreter(loadModelFile("hero_model.tflite"))
            Log.d("VisionEngine", "TFLite Model Engine Initialized (Placeholder)")
        } catch (e: Exception) {
            Log.e("VisionEngine", "Failed to load model", e)
        }
    }

    private var currentPickSlotsLeft = pickSlotsLeft
    private var currentPickSlotsRight = pickSlotsRight

    fun updateLayoutConfig(width: Int, height: Int) {
        val aspectRatio = width.toFloat() / height.toFloat()
        Log.d("VisionEngine", "Updating config for aspect ratio: $aspectRatio")
        
        if (aspectRatio < 1.5f) { // Tablet 4:3 ~1.33
            // Narrower screen: slots might be closer to center or smaller
            currentPickSlotsLeft = pickSlotsLeft.map { 
                Slot(it.name, RectF(it.rect.left + 0.05f, it.rect.top, it.rect.right + 0.05f, it.rect.bottom))
            }
            currentPickSlotsRight = pickSlotsRight.map { 
                Slot(it.name, RectF(it.rect.left - 0.05f, it.rect.top, it.rect.right - 0.05f, it.rect.bottom))
            }
        } else if (aspectRatio > 2.1f) { // Ultra wide 20:9 ~2.22
            // Wider screen: slots might be further at edges
            currentPickSlotsLeft = pickSlotsLeft.map { 
                Slot(it.name, RectF(it.rect.left - 0.02f, it.rect.top, it.rect.right - 0.02f, it.rect.bottom))
            }
            currentPickSlotsRight = pickSlotsRight.map { 
                Slot(it.name, RectF(it.rect.left + 0.02f, it.rect.top, it.rect.right + 0.02f, it.rect.bottom))
            }
        } else {
            currentPickSlotsLeft = pickSlotsLeft
            currentPickSlotsRight = pickSlotsRight
        }
    }

    fun analyze(fullBitmap: Bitmap) {
        val width = fullBitmap.width
        val height = fullBitmap.height
        
        updateLayoutConfig(width, height)
        
        val detectedHeroes = mutableMapOf<String, String>()

        // Process Left Picks
        currentPickSlotsLeft.forEach { slot ->
            val crop = cropSlot(fullBitmap, slot.rect, width, height)
            val heroName = recognizeHero(crop)
            if (heroName != null) detectedHeroes[slot.name] = heroName
        }

        // Process Right Picks
        currentPickSlotsRight.forEach { slot ->
            val crop = cropSlot(fullBitmap, slot.rect, width, height)
            val heroName = recognizeHero(crop)
            if (heroName != null) detectedHeroes[slot.name] = heroName
        }

        // Broadcast results to OverlayService
        OverlayService.updateDetectedHeroes(detectedHeroes)
    }

    private fun cropSlot(bitmap: Bitmap, rect: RectF, w: Int, h: Int): Bitmap {
        val left = (rect.left * w).toInt()
        val top = (rect.top * h).toInt()
        val width = (rect.width() * w).toInt()
        val height = (rect.height() * h).toInt()
        
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun recognizeHero(bitmap: Bitmap): String? {
        // Here we would run TFLite inference
        // For now, returning a mock based on image properties or just null
        // Actual implementation uses: 
        // val tensorImage = TensorImage.fromBitmap(bitmap)
        // interpreter.run(tensorImage.buffer, outputBuffer)
        return null 
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
