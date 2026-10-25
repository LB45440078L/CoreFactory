package io.corefactory.paper.artillery;

import java.util.List;

/**
 * The pages of the artillery list in the Command Center's dialog: six machines to a page, and at least one page, even when no machine is
 * connected. Pages count from 0. Pure, so the paging is tested without a server.
 */
public final class MachinePages {

    /** The machines on one page. */
    public static final int PER_PAGE = 6;

    private MachinePages() {
    }

    /**
     * The number of pages for a list of machines: at least one, even for an empty list.
     *
     * @param items the number of machines, 0 or more
     * @return the page count
     */
    public static int count(int items) {
        return Math.max(1, (items + PER_PAGE - 1) / PER_PAGE);
    }

    /**
     * A page number brought into range for a list: from the first page to the last.
     *
     * @param page  the page asked for
     * @param items the number of machines
     * @return the page, from 0 to the last page
     */
    public static int clamp(int page, int items) {
        return Math.max(0, Math.min(page, count(items) - 1));
    }

    /**
     * Whether a page has a page after it.
     *
     * @param page  the page, counted from 0
     * @param items the number of machines
     */
    public static boolean hasNext(int page, int items) {
        return page < count(items) - 1;
    }

    /**
     * Whether a page has a page before it.
     *
     * @param page the page, counted from 0
     */
    public static boolean hasBack(int page) {
        return page > 0;
    }

    /**
     * The machines on a page, in their order. A page past the end is empty.
     *
     * @param items the machines, in order
     * @param page  the page, counted from 0
     * @return the machines of that page
     */
    public static <T> List<T> slice(List<T> items, int page) {
        int from = Math.min(items.size(), Math.max(0, page) * PER_PAGE);
        int to = Math.min(items.size(), from + PER_PAGE);
        return items.subList(from, to);
    }
}
