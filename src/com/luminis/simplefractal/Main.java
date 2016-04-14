package com.luminis.simplefractal;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

	private static final int MIN_PIXELS = 10;

	private double xMin = -2;
	private double xMax = 1.2;
	private double yMin = -1.2;
	private double yMax = 1.2;
	private double xCenter = xMin + xMax;
	private double yCenter = yMin + yMax;
	private SimpleFloatProperty currentX;
	private SimpleFloatProperty currentY;

	private int canvasWidth = 5120;
	private int canvasHeight = 3200;

	private double adjustedRatio = Math.min(canvasWidth / (xMax - xMin), canvasHeight / (yMax - yMin));

	@Override
	public void start(Stage primaryStage)
	{
		currentX = new SimpleFloatProperty(0);
		currentY = new SimpleFloatProperty(0);

		adjustAspectRatio();
		WritableImage image = new WritableImage(5120,3200);
		PixelWriter writer = image.getPixelWriter();
		double width = image.getWidth();
		double height = image.getHeight();

		ImageView imageView = new ImageView(image);
		imageView.setPreserveRatio(true);
		reset(imageView, width, height);

		ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

		imageView.setOnMouseMoved(e -> {
			Point2D imagePoint = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
			Point2D realPoint = imageToReal(imagePoint);
			currentX.setValue(realPoint.getX());
			currentY.setValue(realPoint.getY());
		});

		Label coordinates = new Label();
		coordinates.textProperty().bind(Bindings.concat("X: ", currentX.asString(), ", Y: ", currentY.asString()));

		imageView.setOnMousePressed(e -> {

			Point2D mousePress = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
			mouseDown.set(mousePress);
		});

		imageView.setOnMouseDragged(e -> {
			Point2D dragPoint = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));
			shift(imageView, dragPoint.subtract(mouseDown.get()));
			mouseDown.set(imageViewToImage(imageView, new Point2D(e.getX(), e.getY())));
		});

		imageView.setOnScroll(e -> {
			double delta = e.getDeltaY();
			Rectangle2D viewport = imageView.getViewport();

			double scale = clamp(Math.pow(1.01, delta),

					// don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
					Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight()),

					// don't scale so that we're bigger than image dimensions:
					Math.max(width / viewport.getWidth(), height / viewport.getHeight())

			);

			Point2D mouse = imageViewToImage(imageView, new Point2D(e.getX(), e.getY()));

			double newWidth = viewport.getWidth() * scale;
			double newHeight = viewport.getHeight() * scale;

			// To keep the visual point under the mouse from moving, we need
			// (x - newViewportMinX) / (x - currentViewportMinX) = scale
			// where x is the mouse X coordinate in the image

			// solving this for newViewportMinX gives

			// newViewportMinX = x - (x - currentViewportMinX) * scale

			// we then clamp this value so the image never scrolls out
			// of the imageview:

			double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale,
					0, width - newWidth);
			double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale,
					0, height - newHeight);

			imageView.setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
		});

		imageView.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				reset(imageView, width, height);
			}
		});

		fractal(writer, 250);

		HBox buttons = createButtons(width, height, imageView);
		Tooltip tooltip = new Tooltip("Scroll to zoom, drag to pan");
		Tooltip.install(buttons, tooltip);

		Pane container = new Pane(imageView);
		container.setPrefSize(950, 600);

		imageView.fitWidthProperty().bind(container.widthProperty());
		imageView.fitHeightProperty().bind(container.heightProperty());
		VBox root = new VBox(container, buttons, coordinates);
		root.setFillWidth(true);
		VBox.setVgrow(container, Priority.ALWAYS);

		primaryStage.setScene(new Scene(root));
		primaryStage.setTitle("Simple Fractal");
		primaryStage.show();
	}

	private HBox createButtons(double width, double height, ImageView imageView) {
		Button full = new Button("Full view");
		full.setOnAction(e -> reset(imageView, width, height));
		HBox buttons = new HBox(10, full);
		buttons.setAlignment(Pos.CENTER);
		buttons.setPadding(new Insets(10));
		return buttons;
	}

	// reset to the top left:
	private void reset(ImageView imageView, double width, double height) {
		imageView.setViewport(new Rectangle2D(0, 0, width, height));
	}

	// shift the viewport of the imageView by the specified delta, clamping so
	// the viewport does not move off the actual image:
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

	// convert mouse coordinates in the imageView to coordinates in the actual image:
	private Point2D imageViewToImage(ImageView imageView, Point2D imageViewCoordinates) {
		double xProportion = imageViewCoordinates.getX() / imageView.getBoundsInLocal().getWidth();
		double yProportion = imageViewCoordinates.getY() / imageView.getBoundsInLocal().getHeight();

		Rectangle2D viewport = imageView.getViewport();
		return new Point2D(
				viewport.getMinX() + xProportion * viewport.getWidth(),
				viewport.getMinY() + yProportion * viewport.getHeight());
	}

	private Point2D imageToReal(Point2D imageCoordinates) {
		double xProportion = imageCoordinates.getX() / canvasWidth;
		double yProportion = imageCoordinates.getY() / canvasHeight;

		return new Point2D(
				xMin + xProportion * ( xMax - xMin ),
				yMax - yProportion * ( yMax - yMin));
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
}
