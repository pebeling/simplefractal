package com.luminis.simplefractal;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

	private Rectangle2D fractalBox = new Rectangle2D(-2.0, -1.2, 3.2, 2.4);

	private int imageWidth = 5120;
	private int imageHeight = 3200;

	private static final int MIN_PIXELS = 50;

	private double ratio = Math.min(imageWidth / fractalBox.getWidth(), imageHeight / fractalBox.getHeight());

	private Rectangle2D adjustedFractalBox = new Rectangle2D(
			-imageWidth / ratio / 2 + fractalBox.getMinX() + fractalBox.getMaxX(),
			-imageHeight / ratio / 2 + fractalBox.getMinY() + fractalBox.getMaxY(),
			imageWidth / ratio,
			imageHeight / ratio);

	@Override
	public void start(Stage primaryStage)
	{
		WritableImage image = new WritableImage(imageWidth, imageHeight);
		PixelWriter writer = image.getPixelWriter();

		ImageView imageView = new ImageView(image);
		imageView.setPreserveRatio(true);
		imageView.setViewport(new Rectangle2D(0, 0, imageWidth, imageHeight));

		SimpleFloatProperty currentX = new SimpleFloatProperty(0);
		SimpleFloatProperty currentY = new SimpleFloatProperty(0);
		imageView.setOnMouseMoved(e -> {
			Point2D imagePoint = imageViewToImageCoordinates(imageView, new Point2D(e.getX(), e.getY()));
			Point2D realPoint = imageToFractalCoordinates(imagePoint);
			currentX.setValue(realPoint.getX());
			currentY.setValue(realPoint.getY());
		});
		Label coordinates = new Label();
		coordinates.textProperty().bind(Bindings.concat("( ", currentX.asString(), ", ", currentY.asString(), " )"));

		ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();
		imageView.setOnMousePressed(e -> {
			Point2D mousePress = imageViewToImageCoordinates(imageView, new Point2D(e.getX(), e.getY()));
			mouseDown.set(mousePress);
		});
		imageView.setOnMouseDragged(e -> {
			Point2D dragPoint = imageViewToImageCoordinates(imageView, new Point2D(e.getX(), e.getY()));
			shift(imageView, dragPoint.subtract(mouseDown.get()));
			mouseDown.set(imageViewToImageCoordinates(imageView, new Point2D(e.getX(), e.getY())));
		});

		imageView.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				imageView.setViewport(new Rectangle2D(0, 0, imageWidth, imageHeight));
			}
		});

		imageView.setOnScroll(e -> {
			double delta = e.getDeltaY();
			Rectangle2D viewport = imageView.getViewport();

			double scale = clamp(Math.pow(0.99, delta), // zoom speed
					Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight()),
					Math.max(imageWidth / viewport.getWidth(), imageHeight / viewport.getHeight())
			);

			Point2D mouse = imageViewToImageCoordinates(imageView, new Point2D(e.getX(), e.getY()));

			double newWidth = viewport.getWidth() * scale;
			double newHeight = viewport.getHeight() * scale;

			double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale, 0, imageWidth - newWidth);
			double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale, 0, imageHeight - newHeight);

			imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
		});

		Timer timer = new Timer();
		timer.start();
		fractal(writer, 500);
		timer.stop();
		System.out.println(timer);

		Pane container = new Pane(imageView);
		container.setPrefSize(950, 600);

		imageView.fitWidthProperty().bind(container.widthProperty());
		imageView.fitHeightProperty().bind(container.heightProperty());
		VBox root = new VBox(container, coordinates);
		root.setFillWidth(true);
		VBox.setVgrow(container, Priority.ALWAYS);

		primaryStage.setScene(new Scene(root));
		primaryStage.setTitle("Simple Fractal");
		primaryStage.show();
	}

	private void shift(ImageView imageView, Point2D delta) {
		Rectangle2D viewport = imageView.getViewport();

		double width = imageView.getImage().getWidth() ;
		double height = imageView.getImage().getHeight() ;

		double maxX = width - viewport.getWidth();
		double maxY = height - viewport.getHeight();

		double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
		double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

		imageView.setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
	}

	private double clamp(double value, double min, double max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}

	private Point2D imageViewToImageCoordinates(ImageView imageView, Point2D imageViewCoordinates) {
		double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
		double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

		Rectangle2D viewport = imageView.getViewport();
		return new Point2D(
				viewport.getMinX() + xProportion * viewport.getWidth(),
				viewport.getMinY() + yProportion * viewport.getHeight());
	}

	private Point2D imageToFractalCoordinates(Point2D imageCoordinates) {
		double xProportion = imageCoordinates.getX() / imageWidth;
		double yProportion = imageCoordinates.getY() / imageHeight;

		return new Point2D(
				adjustedFractalBox.getMinX() + xProportion * adjustedFractalBox.getWidth(),
				adjustedFractalBox.getMaxY() - yProportion * adjustedFractalBox.getHeight());
	}

	public static void main(String[] args) {
		launch(args);
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

		for(int xCanvas = 0; xCanvas < imageWidth; xCanvas++) {
			x = xCanvas / ratio + adjustedFractalBox.getMinX();
			for(int yCanvas = 0; yCanvas < imageHeight; yCanvas++) {
				y = -yCanvas / ratio + adjustedFractalBox.getMaxY();

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
