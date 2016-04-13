package com.luminis.simplefractal;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {
	private double xMin = -2;
	private double xMax = 1.2;
	private double yMin = -1.2;
	private double yMax = 1.2;
	private double xCenter = xMin + xMax;
	private double yCenter = yMin + yMax;

	private int canvasHorizontal = 600;
	private int canvasVertical = 500;

	private double adjustedRatio = Math.min(canvasHorizontal / (xMax - xMin), canvasVertical / (yMax - yMin));

	@Override
	public void start(Stage primaryStage) throws Exception{
		adjustAspectRatio();

		primaryStage.setTitle("Simple Mandelbrot Fractal");
		Group root = new Group();

		WritableImage image = new WritableImage(canvasHorizontal, canvasVertical);
		PixelWriter writer = image.getPixelWriter();

		Timer timer = new Timer();
		timer.start();
		fractal(writer, 50);
		timer.stop();
		System.out.println(timer);

		ImageView imageView = new ImageView(image);
		root.getChildren().add(imageView);
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

	private void adjustAspectRatio(){
		xMin = -canvasHorizontal / adjustedRatio / 2 + xCenter;
		xMax = canvasHorizontal / adjustedRatio / 2 + xCenter;

		yMin = -canvasVertical / adjustedRatio / 2 + yCenter;
		yMax = canvasVertical / adjustedRatio / 2 + yCenter;
	}

	private void fractal(PixelWriter writer, int iterationDepth){
		Color color;
		double tmp;
		double x, y;

		double xOrbit;
		double yOrbit;
		double tOrbit;

		double xOrbitSq;
		double yOrbitSq;

		for(int xCanvas = 0; xCanvas < canvasHorizontal; xCanvas++) {
			x = xCanvas / adjustedRatio + xMin;
			for(int yCanvas = 0; yCanvas < canvasVertical; yCanvas++) {
				y = yCanvas / adjustedRatio + yMin;

				xOrbit = 0;
				yOrbit = 0;

				xOrbitSq = 0;
				yOrbitSq = 0;

				double i;
				for (i = 0; i < iterationDepth && xOrbitSq + yOrbitSq < 10; i++) {
					xOrbitSq = xOrbit * xOrbit;
					yOrbitSq = yOrbit * yOrbit;

					tOrbit = xOrbit;
					xOrbit = xOrbitSq - yOrbitSq + x;
					yOrbit = 2 * tOrbit * yOrbit + y;
				}

				if(i < iterationDepth) {
					tmp = 1 - i / iterationDepth;
					color = Color.color(tmp, tmp * tmp, tmp * tmp * tmp, 1.0);
				} else {
					color = Color.BLACK;
//					tmp = Math.min(xOrbitSq + yOrbitSq, 10) / 10;
//					tmp = Math.sqrt(tmp);
//					color = Color.color(tmp, tmp * tmp, tmp * tmp * tmp, 1.0);
				}
				writer.setColor(xCanvas, yCanvas, color);
			}
		}
	}

	private class Timer {
		private long startTime;
		private long endTime;
		private boolean running = false;
		private boolean hasTime = false;

		void start() {
			if(!running) {
				running = true;
				startTime = System.nanoTime();
			}
		}
		void stop() {
			if(running) {
				endTime = System.nanoTime();
				running = false;
				hasTime = true;
			}
		}
		@Override
		public String toString() {
			if(hasTime) {
				long result = ( endTime - startTime ) / 1000000;
				return "" + result + " ms";
			} else return "";
		}
	}
}
