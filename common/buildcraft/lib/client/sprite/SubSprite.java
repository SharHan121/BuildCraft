package buildcraft.lib.client.sprite;

public class SubSprite implements ISprite {

    private final ISprite delegate;
    private final double uMin, vMin, uMax, vMax;

    public SubSprite(ISprite delegate, double uMin, double vMin, double uMax, double vMax) {
        this.delegate = delegate;
        this.uMin = uMin;
        this.vMin = vMin;
        this.uMax = uMax;
        this.vMax = vMax;
    }

    @Override
    public void bindTexture() {
        delegate.bindTexture();
    }

    @Override
    public double getInterpU(double u) {
        double iu = uMin * (1 - u) + uMax * u;
        return delegate.getInterpU(iu);
    }

    @Override
    public double getInterpV(double v) {
        double iv = vMin * (1 - v) + vMax * v;
        return delegate.getInterpV(iv);
    }
}
