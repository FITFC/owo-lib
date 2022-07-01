package io.wispforest.owo.ui;

import io.wispforest.owo.ui.definitions.*;
import io.wispforest.owo.util.Observable;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * The reference implementation of the {@link ParentComponent} interface,
 * serving as a base for all parent components on owo-ui. If you need your own parent
 * component, it is often beneficial to subclass one of owo-ui's existing layout classes,
 * especially {@link io.wispforest.owo.ui.layout.WrappingParentComponent} is often useful
 */
public abstract class BaseParentComponent extends BaseComponent implements ParentComponent {

    protected Observable<VerticalAlignment> verticalAlignment = Observable.of(VerticalAlignment.TOP);
    protected Observable<HorizontalAlignment> horizontalAlignment = Observable.of(HorizontalAlignment.LEFT);

    protected AnimatableProperty<Insets> padding = AnimatableProperty.of(Insets.none());

    protected @Nullable FocusHandler focusHandler = null;

    protected Surface surface = Surface.BLANK;
    protected boolean allowOverflow = false;

    protected BaseParentComponent(Sizing horizontalSizing, Sizing verticalSizing) {
        this.horizontalSizing.set(horizontalSizing);
        this.verticalSizing.set(verticalSizing);

        Observable.observeAll(this::updateLayout, horizontalAlignment, verticalAlignment, padding);
    }

    @Override
    public void draw(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta) {
        this.surface.draw(matrices, this);
    }

    @Override
    public @Nullable FocusHandler focusHandler() {
        if (this.focusHandler == null) {
            return super.focusHandler();
        } else {
            return this.focusHandler;
        }
    }

    @Override
    public ParentComponent verticalAlignment(VerticalAlignment alignment) {
        this.verticalAlignment.set(alignment);
        return this;
    }

    @Override
    public VerticalAlignment verticalAlignment() {
        return this.verticalAlignment.get();
    }

    @Override
    public ParentComponent horizontalAlignment(HorizontalAlignment alignment) {
        this.horizontalAlignment.set(alignment);
        return this;
    }

    @Override
    public HorizontalAlignment horizontalAlignment() {
        return this.horizontalAlignment.get();
    }

    @Override
    public ParentComponent padding(Insets padding) {
        this.padding.set(padding);
        this.updateLayout();
        return this;
    }

    @Override
    public AnimatableProperty<Insets> padding() {
        return this.padding;
    }

    @Override
    public ParentComponent allowOverflow(boolean allowOverflow) {
        this.allowOverflow = allowOverflow;
        return this;
    }

    @Override
    public boolean allowOverflow() {
        return this.allowOverflow;
    }

    @Override
    public ParentComponent surface(Surface surface) {
        this.surface = surface;
        return this;
    }

    @Override
    public Surface surface() {
        return this.surface;
    }

    @Override
    public void mount(ParentComponent parent, int x, int y) {
        super.mount(parent, x, y);
        if (parent == null && this.focusHandler == null) {
            this.focusHandler = new FocusHandler(this);
        }
    }

    @Override
    public void inflate(Size space) {
        this.space = space;

        for (var child : this.children()) {
            child.onDismounted(DismountReason.LAYOUT_INFLATION);
        }

        super.inflate(space);
        this.layout(space);
        super.inflate(space);
    }

    protected void updateLayout() {
        if (!this.hasParent()) return;

        var previousSize = this.fullSize();
        this.inflate(this.space);

        if (!previousSize.equals(this.fullSize()) && this.parent != null) {
            this.parent.onChildMutated(this);
        }
    }

    @Override
    public void onChildMutated(Component child) {
        var previousSize = this.fullSize();
        this.inflate(this.space);

        if (this.parent != null && !previousSize.equals(this.fullSize())) {
            this.parent.onChildMutated(this);
        }
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return false;
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (this.focusHandler != null) {
            this.focusHandler.updateClickFocus(mouseX, mouseY);
        }

        return ParentComponent.super.onMouseClick(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseRelease(double mouseX, double mouseY, int button) {
        if (this.focusHandler != null && this.focusHandler.focused() != null) {
            final var focused = this.focusHandler.focused();
            return focused.onMouseRelease(mouseX - focused.x(), mouseY - focused.y(), button);
        } else {
            return super.onMouseRelease(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (this.focusHandler != null && this.focusHandler.focused() != null) {
            final var focused = this.focusHandler.focused();
            return focused.onMouseDrag(mouseX - focused.x(), mouseY - focused.y(), deltaX, deltaY, button);
        } else {
            return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
        }
    }

    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (this.focusHandler == null) return false;

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            this.focusHandler.cycle((modifiers & GLFW.GLFW_MOD_SHIFT) == 0);
        } else if ((keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP)
                && (modifiers & GLFW.GLFW_MOD_ALT) != 0) {
            this.focusHandler.moveFocus(keyCode);
        } else if (this.focusHandler.focused != null) {
            return this.focusHandler.focused.onKeyPress(keyCode, scanCode, modifiers);
        }

        return false;
    }

    @Override
    public boolean onCharTyped(char chr, int modifiers) {
        if (this.focusHandler == null) return false;

        if (this.focusHandler.focused != null) {
            return this.focusHandler.focused.onCharTyped(chr, modifiers);
        }

        return false;
    }

    @Override
    public void setX(int x) {
        int offset = x - this.x;
        super.setX(x);

        for (var child : this.children()) {
            child.setX(child.x() + offset);
        }
    }

    @Override
    public void setY(int y) {
        int offset = y - this.y;
        super.setY(y);

        for (var child : this.children()) {
            child.setY(child.y() + offset);
        }
    }

    /**
     * @return The offset from the origin of this component
     * at which children can start to be mounted. Accumulates
     * padding as well as padding from content sizing
     */
    protected Size childMountingOffset() {
        var padding = this.padding.get();
        var horizontalSizing = this.horizontalSizing.get();
        var verticalSizing = this.verticalSizing.get();

        return Size.of(
                padding.left() + (horizontalSizing.method == Sizing.Method.CONTENT ? horizontalSizing.value : 0),
                padding.top() + (verticalSizing.method == Sizing.Method.CONTENT ? verticalSizing.value : 0)
        );
    }

    /**
     * Mount a child using the given mounting function if its positioning
     * is equal to {@link Positioning#layout()}, or according to its
     * intrinsic positioning otherwise
     *
     * @param child      The child to mount
     * @param space      The available space for the to expand into
     * @param layoutFunc The mounting function for components which follow the layout
     */
    protected void mountChild(@Nullable Component child, Size space, Consumer<Component> layoutFunc) {
        if (child == null) return;

        final var positioning = child.positioning().get();
        final var componentMargins = child.margins().get();
        final var padding = this.padding.get();

        switch (positioning.type) {
            case LAYOUT -> layoutFunc.accept(child);
            case ABSOLUTE -> {
                child.inflate(space);
                child.mount(
                        this,
                        this.x + positioning.x + componentMargins.left() + padding.left(),
                        this.y + positioning.y + componentMargins.top() + padding.top()
                );
            }
            case RELATIVE -> {
                child.inflate(space);
                child.mount(
                        this,
                        this.x + padding.left() + componentMargins.left() + Math.round((positioning.x / 100f) * (this.width() - child.fullSize().width() - padding.horizontal())),
                        this.y + padding.top() + componentMargins.top() + Math.round((positioning.y / 100f) * (this.height() - child.fullSize().height() - padding.vertical()))
                );
            }
        }
    }


    /**
     * If {@code clip} is {@code true}, execute the given drawing
     * function and clip overflowing children to the bounding box
     * of the component, otherwise draw normally
     *
     * @param clip     Whether to clip overflowing components
     * @param drawFunc The drawing function to run
     */
    protected void drawClipped(MatrixStack matrices, boolean clip, Runnable drawFunc) {
        if (clip) {
            var padding = this.padding.get();
            ScissorStack.push(this.x + padding.left(), this.y + padding.top(), this.width - padding.horizontal(), this.height - padding.vertical(), matrices);
        }

        drawFunc.run();

        if (clip) {
            ScissorStack.pop();
        }
    }

    /**
     * Calculate the space for child inflation. If a given axis
     * is content-sized, return the respective value from {@code thisSpace}
     *
     * @param thisSpace The space for layout inflation of this widget
     * @return The available space for child inflation
     */
    protected Size calculateChildSpace(Size thisSpace) {
        final var padding = this.padding.get();

        return Size.of(
                this.horizontalSizing.get().method == Sizing.Method.CONTENT ? thisSpace.width() : this.width - padding.horizontal(),
                this.verticalSizing.get().method == Sizing.Method.CONTENT ? thisSpace.height() : this.height - padding.vertical()
        );
    }

    @Override
    public BaseParentComponent positioning(Positioning positioning) {
        return (BaseParentComponent) super.positioning(positioning);
    }

    @Override
    public BaseParentComponent margins(Insets margins) {
        return (BaseParentComponent) super.margins(margins);
    }
}
