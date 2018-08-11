//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package shape3d;

import processing.core.PImage;
import shapes3d.EndCapBase;
import shapes3d.utils.Textures;

public class Mesh2DCoreWithCaps extends Mesh2DCore {
    public EndCapBase startEC;
    public EndCapBase endEC;

    public Mesh2DCoreWithCaps() {
    }

    protected void calcShape() {
    }

    public void visible(boolean var1, int var2) {
        if(this.isFlagSet(var2, 50331650)) {
            this.startEC.visible = var1;
        }

        if(this.isFlagSet(var2, 50331649)) {
            this.endEC.visible = var1;
        }

    }

    public void drawMode(int var1, int var2) {
        if(this.isFlagSet(var2, 50331650)) {
            this.startEC.drawMode(var1);
        }

        if(this.isFlagSet(var2, 50331649)) {
            this.endEC.drawMode(var1);
        }

    }

    public void fill(int var1, int var2) {
        if(this.isFlagSet(var2, 50331650)) {
            this.startEC.col = var1;
        }

        if(this.isFlagSet(var2, 50331649)) {
            this.endEC.col = var1;
        }

    }

    public void strokeWeight(float var1, int var2) {
        if(this.isFlagSet(var2, 50331650)) {
            this.startEC.sweight = var1;
        }

        if(this.isFlagSet(var2, 50331649)) {
            this.endEC.sweight = var1;
        }

    }

    public void stroke(int var1, int var2) {
        if(this.isFlagSet(var2, 50331650)) {
            this.startEC.scol = var1;
        }

        if(this.isFlagSet(var2, 50331649)) {
            this.endEC.scol = var1;
        }

    }

    public void setTexture(String var1, int var2) {
        PImage var3 = Textures.loadImage(this.app, var1);
        this.setTexture(var3, var2);
    }

    public void setTexture(PImage var1, int var2) {
        if(var1 != null) {
            if(this.isFlagSet(var2, 50331650)) {
                this.startEC.capSkin = var1;
            }

            if(this.isFlagSet(var2, 50331649)) {
                this.endEC.capSkin = var1;
            }
        }

    }
}
