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

	private int canvasWidth = 600;
	private int canvasHeight = 500;
	private double aspectRatio = canvasWidth / canvasHeight;

	private double adjustedRatio = Math.min(canvasWidth / (xMax - xMin), canvasHeight / (yMax - yMin));

	@Override
	public void start(Stage primaryStage) throws Exception{
		adjustAspectRatio();

		primaryStage.setTitle("Simple Mandelbrot Fractal");
		Group root = new Group();

		WritableImage image = new WritableImage(canvasWidth, canvasHeight);
		PixelWriter writer = image.getPixelWriter();

		Timer timer = new Timer();
		timer.start();
		fractal(writer, 100);
		timer.stop();
		System.out.println(timer);

		ImageView imageView = new ImageView(image);
		imageView.setPreserveRatio(true);

		root.getChildren().add(imageView);
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
		imageView.fitWidthProperty().bind(scene.widthProperty());
	}

	public static void main(String[] args) {
		launch(args);
	}

	private void adjustAspectRatio(){
		xMin = -canvasWidth / adjustedRatio / 2 + xCenter;
		xMax = canvasWidth / adjustedRatio / 2 + xCenter;

		yMin = -canvasHeight / adjustedRatio / 2 + yCenter;
		yMax = canvasHeight / adjustedRatio / 2 + yCenter;
	}

	private void fractal(PixelWriter writer, int iterationDepth){
		Color color;
		double tmp;
		double x, y;
		double limit = 5.0;

		double xOrbit;
		double yOrbit;
		double tOrbit;

		double xOrbitSq;
		double yOrbitSq;

		for(int xCanvas = 0; xCanvas < canvasWidth; xCanvas++) {
			x = xCanvas / adjustedRatio + xMin;
			for(int yCanvas = 0; yCanvas < canvasHeight; yCanvas++) {
				y = yCanvas / adjustedRatio + yMin;

				xOrbit = 0;
				yOrbit = 0;

				xOrbitSq = 0;
				yOrbitSq = 0;

				double i;
				for (i = 0; i < iterationDepth && xOrbitSq + yOrbitSq < limit; i++) {
					xOrbitSq = xOrbit * xOrbit;
					yOrbitSq = yOrbit * yOrbit;

					tOrbit = xOrbit;
					xOrbit = xOrbitSq - yOrbitSq + x;
					yOrbit = 2 * tOrbit * yOrbit + y;
				}

				if(i < iterationDepth) {
					tmp = 1 - Math.sqrt(i) / Math.sqrt(iterationDepth);
					color = Color.color(tmp, tmp * tmp, tmp * tmp * tmp, 1.0);
				} else {
					//color = Color.BLACK;
					tmp = Math.min(xOrbitSq + yOrbitSq, limit) / limit;
					tmp = Math.sqrt(tmp);
					if (tmp > 0.9) tmp = 0;
					color = Color.color(tmp * tmp, 0, tmp, 1.0);
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
