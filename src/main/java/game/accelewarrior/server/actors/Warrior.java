package game.accelewarrior.server.actors;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Warrior implements Json.Serializable{
    private String id;

    private float x;
    private float y;
    private int speed = 300;

    private float accelerometerX;
    private float accelerometerY;
    private boolean justTouched;

    public void act(float delta) {
        float stepLength = speed * delta;

        x = accelerometerY * stepLength;
        y = accelerometerX * stepLength;
    }

    @Override
    public void write(Json json) {
        json.writeValue("x", x);
        json.writeValue("y", y);
        json.writeValue("id", id);
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public float getAccelerometerX() {
        return accelerometerX;
    }

    public void setAccelerometerX(float accelerometerX) {
        this.accelerometerX = accelerometerX;
    }

    public float getAccelerometerY() {
        return accelerometerY;
    }

    public void setAccelerometerY(float accelerometerY) {
        this.accelerometerY = accelerometerY;
    }

    public boolean isJustTouched() {
        return justTouched;
    }

    public void setJustTouched(boolean justTouched) {
        this.justTouched = justTouched;
    }
}
