// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth,
// inexhaustible as the great rivers.
// When they come to an end,
// they begin again,
// like the days and months;
// they die and are reborn,
// like the four seasons."
//
// - Sun Tsu,
// "The Art of War"

package com.theartofdev.edmodo.cropper;

import android.graphics.RectF;

/**
 * Handler from crop window stuff, moving and knowing possition.
 */
final class CropWindowHandler {

    //region: Fields and Consts

    private final RectF mEdges = new RectF();

    /**
     * Rectangle used to return the edges rectangle without ability to change it and without creating new all the time.
     */
    private final RectF mGetEdges = new RectF();
    //endregion

    /**
     * Get the left/top/right/bottom coordinates of the crop window.
     */
    public RectF getRect() {
        mGetEdges.set(mEdges);
        return mGetEdges;
    }

    /**
     * Set the left/top/right/bottom coordinates of the crop window.
     */
    public void setRect(RectF rect) {
        mEdges.set(rect);
    }

    /**
     * Indicates whether the crop window is small enough that the guidelines
     * should be shown. Public because this function is also used to determine
     * if the center handle should be focused.
     *
     * @return boolean Whether the guidelines should be shown or not
     */
    public boolean showGuidelines() {
        return !(mEdges.width() < CropDefaults.DEFAULT_SHOW_GUIDELINES_LIMIT
                || mEdges.height() < CropDefaults.DEFAULT_SHOW_GUIDELINES_LIMIT);
    }

    /**
     * Determines which, if any, of the handles are pressed given the touch
     * coordinates, the bounding box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    public CropWindowMoveHandler getMoveHandler(float x, float y, float targetRadius, CropImageView.CropShape cropShape) {
        CropWindowMoveHandler.Type type = cropShape == CropImageView.CropShape.OVAL
                ? getOvalPressedMoveType(x, y)
                : getRectanglePressedMoveType(x, y, targetRadius);
        return type != null ? new CropWindowMoveHandler(type, this, x, y) : null;
    }

    //region: Private methods

    /**
     * Determines which, if any, of the handles are pressed given the touch
     * coordinates, the bounding box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private CropWindowMoveHandler.Type getRectanglePressedMoveType(float x, float y, float targetRadius) {
        CropWindowMoveHandler.Type moveType = null;

        // Note: corner-handles take precedence, then side-handles, then center.
        if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_LEFT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.right, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_RIGHT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.left, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, mEdges.right, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && focusCenter()) {
            moveType = CropWindowMoveHandler.Type.CENTER;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, mEdges.left, mEdges.right, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.BOTTOM;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, mEdges.left, mEdges.top, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.LEFT;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, mEdges.right, mEdges.top, mEdges.bottom, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.RIGHT;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom) && !focusCenter()) {
            moveType = CropWindowMoveHandler.Type.CENTER;
        }

        return moveType;
    }

    /**
     * Determines which, if any, of the handles are pressed given the touch
     * coordinates, the bounding box/oval, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private CropWindowMoveHandler.Type getOvalPressedMoveType(float x, float y) {

        /*
           Use a 6x6 grid system divided into 9 "handles", with the center the biggest region. While
           this is not perfect, it's a good quick-to-ship approach.

           TL T T T T TR
            L C C C C R
            L C C C C R
            L C C C C R
            L C C C C R
           BL B B B B BR
        */

        float cellLength = mEdges.width() / 6;
        float leftCenter = mEdges.left + cellLength;
        float rightCenter = mEdges.left + (5 * cellLength);

        float cellHeight = mEdges.height() / 6;
        float topCenter = mEdges.top + cellHeight;
        float bottomCenter = mEdges.top + 5 * cellHeight;

        CropWindowMoveHandler.Type moveType;
        if (x < leftCenter) {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP_LEFT;
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.LEFT;
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT;
            }
        } else if (x < rightCenter) {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP;
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.CENTER;
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM;
            }
        } else {
            if (y < topCenter) {
                moveType = CropWindowMoveHandler.Type.TOP_RIGHT;
            } else if (y < bottomCenter) {
                moveType = CropWindowMoveHandler.Type.RIGHT;
            } else {
                moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT;
            }
        }

        return moveType;
    }

    /**
     * Determines if the specified coordinate is in the target touch zone for a
     * corner handle.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param handleX the x-coordinate of the corner handle
     * @param handleY the y-coordinate of the corner handle
     * @param targetRadius the target radius in pixels
     * @return true if the touch point is in the target touch zone; false
     * otherwise
     */
    private static boolean isInCornerTargetZone(float x, float y, float handleX, float handleY, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius;
    }

    /**
     * Determines if the specified coordinate is in the target touch zone for a
     * horizontal bar handle.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param handleXStart the left x-coordinate of the horizontal bar handle
     * @param handleXEnd the right x-coordinate of the horizontal bar handle
     * @param handleY the y-coordinate of the horizontal bar handle
     * @param targetRadius the target radius in pixels
     * @return true if the touch point is in the target touch zone; false
     * otherwise
     */
    private static boolean isInHorizontalTargetZone(float x, float y, float handleXStart, float handleXEnd, float handleY, float targetRadius) {
        return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius;
    }

    /**
     * Determines if the specified coordinate is in the target touch zone for a
     * vertical bar handle.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param handleX the x-coordinate of the vertical bar handle
     * @param handleYStart the top y-coordinate of the vertical bar handle
     * @param handleYEnd the bottom y-coordinate of the vertical bar handle
     * @param targetRadius the target radius in pixels
     * @return true if the touch point is in the target touch zone; false
     * otherwise
     */
    private static boolean isInVerticalTargetZone(float x, float y, float handleX, float handleYStart, float handleYEnd, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd;
    }

    /**
     * Determines if the specified coordinate falls anywhere inside the given
     * bounds.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @return true if the touch point is inside the bounding rectangle; false
     * otherwise
     */
    private static boolean isInCenterTargetZone(float x, float y, float left, float top, float right, float bottom) {
        return x > left && x < right && y > top && y < bottom;
    }

    /**
     * Determines if the cropper should focus on the center handle or the side
     * handles. If it is a small image, focus on the center handle so the user
     * can move it. If it is a large image, focus on the side handles so user
     * can grab them. Corresponds to the appearance of the
     * RuleOfThirdsGuidelines.
     *
     * @return true if it is small enough such that it should focus on the
     * center; less than show_guidelines limit
     */
    private boolean focusCenter() {
        return !showGuidelines();
    }
    //endregion
}