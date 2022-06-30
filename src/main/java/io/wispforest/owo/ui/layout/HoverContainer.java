package io.wispforest.owo.ui.layout;

import io.wispforest.owo.ui.definitions.Animation;
import io.wispforest.owo.ui.definitions.Component;
import io.wispforest.owo.ui.definitions.Sizing;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Consumer;
import java.util.function.Function;

public class HoverContainer<T extends Component> extends ShrinkWrapParentComponent<T> {

    protected final Consumer<T> onMouseEnter;
    protected final Consumer<T> onMouseLeave;

    protected boolean hovered = false;

    protected HoverContainer(T child, Consumer<T> onMouseEnter, Consumer<T> onMouseLeave) {
        super(Sizing.content(), Sizing.content(), child);
        this.onMouseEnter = onMouseEnter;
        this.onMouseLeave = onMouseLeave;
    }

    public static <T extends Component> HoverContainer<T> forAnimation(T child, Function<T, Animation<?>> animationMaker) {
        var animation = animationMaker.apply(child);
        return new HoverContainer<>(child, t -> animation.reverse(), t -> animation.reverse());
    }

    @Override
    public void draw(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(matrices, mouseX, mouseY, partialTicks, delta);

        boolean nowHovered = this.isInBoundingBox(mouseX, mouseY);
        if (this.hovered != nowHovered) {
            this.hovered = nowHovered;

            (this.hovered ? this.onMouseEnter : this.onMouseLeave).accept(this.child);
        }

        this.drawClipped(matrices, !this.allowOverflow, () -> this.child.draw(matrices, mouseX, mouseY, partialTicks, delta));
    }
}