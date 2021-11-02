package jp.shosakaguchi.searchvp.tools

import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX
import org.opencv.imgproc.Imgproc.LINE_AA
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


/**
 * VP関連ツール
 * @author Sho Sakaguchi 2021/10/31
 */
class Vp {

    /**
     * エッジ検出
     * @param image OpenCVImage
     * @return (x,y),(x1,y1)...(xn,yn)リストの結果の並列処理結果（Double型リスト）
     *
     */
    private fun houghTransForm(image: Mat): List<OpenCvPointsForLine> {
        val convertedColorImage = Mat()
        val morphologyExImage = Mat()
        val cannyImage = Mat()
        val houghLinesImage = Mat()

        val kernelThreshold = 15.00
        image.copyTo(convertedColorImage)
        Imgproc.cvtColor(image, convertedColorImage, Imgproc.COLOR_BGR2GRAY)

        val kernel = Mat()
        kernel.setTo(Scalar(kernelThreshold, kernelThreshold))

        convertedColorImage.copyTo(morphologyExImage)
        Imgproc.morphologyEx(morphologyExImage, morphologyExImage, Imgproc.MORPH_OPEN, kernel)

        morphologyExImage.copyTo(cannyImage)
        Imgproc.Canny(cannyImage, cannyImage, 2.00, 5.00, 3)

        cannyImage.copyTo(houghLinesImage)
        Imgproc.HoughLines(houghLinesImage, houghLinesImage, 1.00, Math.PI / 180, 5)
        return calcEndPoints(houghLinesImage)

    }

    /**
     * 点と点を結んで線にする
     * @param lines 線の集合体データ
     *
     * @return 分解済みの線のデータ構造（EL）
     */
    private fun calcEndPoints(lines: Mat): List<OpenCvPointsForLine> {
        val result = mutableListOf<OpenCvPointsForLine>()
        for (i in 0 until lines.rows()) {
            val data: DoubleArray = lines.get(i, 0)
            val rho1 = data[0]
            val theta1 = data[1]
            val cosTheta = cos(theta1)
            val sinTheta = sin(theta1)
            val x0 = cosTheta * rho1
            val y0 = sinTheta * rho1
            val pt1 = Point(x0 + 10000 * -sinTheta, y0 + 10000 * cosTheta)
            val pt2 = Point(x0 - 10000 * -sinTheta, y0 - 10000 * cosTheta)
            result.add(OpenCvPointsForLine(pt1, pt2))
        }
        return result
    }

    /**
     * 消失点探索アルゴリズム
     * @param image 画像
     * @param gridSize 画像サイズ
     * @param intersections VP候補点
     *
     * @return VPの座標
     */
    private fun findVanishingPoint(
        image: Mat,
        gridSize: Double,
        intersections: List<Point>
    ): Point {
        val imageWidth = image.width()
        val imageHeight = image.height()

        val gridRows = ((imageHeight / gridSize) + 1).toInt()
        val gridColumns = ((imageWidth / gridSize) + 1).toInt()

        // 初期設定
        // 既存のEL線数
        var maxIntersections = 0
        // 探索した中で一番正確なセル
        var bestCell = Point(0.0, 0.0)

        val rectImage = Mat()


        // 入力画像のデカルト積を1ドットずつ処理する
        // デカルトを算出
        val cartesian = cartProd(IntRange(0, gridColumns), IntRange(0, gridRows)).toList()
        image.copyTo(rectImage)
        // デカルト単位でデカルト積を参照し、デカルト単位の中で最もVP候補が多いセルを信用する
        try {
            cartesian.map { list ->
                list.forEachIndexed { index, point ->
                    val cellLeft = index * gridSize
                    val cellRight = (index + 1) * gridSize
                    val cellBottom = point * gridSize
                    val cellTop = (point + 1) * gridSize

                    // 現在の交差する線のセクション数
                    // グリッドに収まり切るEL線の数
                    val currentIntersections: Int = intersections.filter {
                        it.x in cellLeft..cellRight && it.y in cellBottom..cellTop
                    }.size

                    // 現在のセルが、前のセルよりも多くの交差がある場合、最大交差数を現在の交差数で置き換えセルの座標を求める（精度向上用のロジック）
                    if (currentIntersections > maxIntersections) {
                        maxIntersections = currentIntersections
                        bestCell = (Point(
                            doubleArrayOf(
                                ((cellLeft + cellRight) / 2),
                                ((cellBottom + cellTop) / 2)
                            )
                        ))
                        Log.d("Debug", "VP is " + bestCell.x + ", " + bestCell.y)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // image書き出し
        // VPを表現する枠を算出する
        val rectX1 = (bestCell.x - (gridSize / 2))
        val rectY1 = (bestCell.y - (gridSize / 2))
        val rectX2 = (bestCell.x + (gridSize / 2))
        val rectY2 = (bestCell.y + (gridSize / 2))


        // VPを描画
        Imgproc.rectangle(
            rectImage,
            Point(rectX1, rectY1),
            Point(rectX2, rectY2),
            Scalar(0.00, 0.00, 255.00),
            10
        )
        Imgproc.putText(
            rectImage, "x", bestCell, FONT_HERSHEY_SIMPLEX, 0.35,
            Scalar(255.00, 255.00, 255.00), LINE_AA
        )

        // ファイルへ書き出し
        val filePath = "/storage/emulated/0/Download/" + System.currentTimeMillis() + "temp.jpeg"
        imageWrite(filePath, rectImage)

        return bestCell
    }

    /**
     * EL線リストの中からランダムに複数の線を返却する
     *
     * @param intersectionLines EL線候補リスト
     * @param size 上限サイズ
     */
    private fun sampleLines(
        intersectionLines: List<OpenCvPointsForLine>,
        size: Int
    ): List<OpenCvPointsForLine> {
        var randomSize: Int
        randomSize = intersectionLines.size
        if (size <= intersectionLines.size) {
            randomSize = size
        }
        // 先ず要素の順番をランダムにする
        val shuffled: List<OpenCvPointsForLine> =
            ArrayList(intersectionLines)
        shuffled.shuffled()

        return shuffled.subList(0, randomSize)
    }

    /**
     * 2本の線から交差する点を導出する
     * @param line1　線１
     * @param line2　線２
     *
     * @return 交差する点（VP候補）
     */
    private fun lineIntersection(line1: OpenCvPointsForLine, line2: OpenCvPointsForLine): Point? {
        // x0 Point 1 of Line 1
        // y0 Point 1 of Line 1
        // x1 Point 2 of Line 1
        // y1 Point 2 of Line 1
        // x2 Point 1 of Line 2
        // y2 Point 1 of Line 2
        // x3 Point 2 of Line 2
        // y3 Point 2 of Line 2
        val x0: Double = line1.point0.x
        val y0: Double = line1.point0.y
        val x1: Double = line1.point1.x
        val y1: Double = line1.point1.y
        val x2: Double = line2.point0.x
        val y2: Double = line2.point0.y
        val x3: Double = line2.point1.x
        val y3: Double = line2.point1.y
        val d = (x0 - x1) * (y2 - y3) - (y0 - y1) * (x2 - x3)
        if (d == 0.0) {
            return null
        }
        val xi = ((x2 - x3) * (x0 * y1 - y0 * x1) - (x0 - x1) * (x2 * y3 - y2 * x3)) / d
        val yi = ((y2 - y3) * (x0 * y1 - y0 * x1) - (y0 - y1) * (x2 * y3 - y2 * x3)) / d
        return Point(xi, yi)
    }

    /**
     * 複数の線から交差する複数のVP候補を導出する
     */
    private fun findIntersections(lines: List<OpenCvPointsForLine>): List<Point> {
        val intersections = mutableListOf<Point>()
        lines.forEachIndexed { _, openCvPointsForLine1 ->
            val lines2 = lines.drop(1)
            lines2.forEach { openCvPointsForLine2 ->
                if (openCvPointsForLine1 != openCvPointsForLine2) {
                    // VP候補をリストに追加する
                    lineIntersection(openCvPointsForLine1, openCvPointsForLine2)?.let {
                        intersections.add(it)
                    }
                }
            }
        }
        return intersections
    }

    /**
     * VPを描画しファイルに出力する
     */
    fun imageProcessForVanishingPoint(filePath: String) {
        val image = Imgcodecs.imread(filePath)
        val houghLines = houghTransForm(image)
        houghLines.let {
            val randomSample = sampleLines(it, 1000)
            val intersections = findIntersections(randomSample)
            if (intersections.isNotEmpty()) {
                val gridSize = min(image.height(), image.width())
                val vanishingPoint = findVanishingPoint(image, gridSize.toDouble(), intersections)
                Log.d("Debug", "vanishing point is : " + vanishingPoint.x + ", " + vanishingPoint.y)
            }
        }
    }

    /**
     * イメージをファイルに書き出し
     * @param filePath ファイル名
     * @param image OpenCV対応のイメージ
     */
    private fun imageWrite(filePath: String, image: Mat) {
        Imgcodecs.imwrite(filePath, image)
    }
}

/**
 * 線データ（2点間距離）
 */
data class OpenCvPointsForLine(val point0: Point, val point1: Point)