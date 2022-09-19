package com.activiti.extension.bean.utils;

import com.activiti.extension.model.Pagination;
import com.activiti.model.common.AbstractRepresentation;
import com.activiti.model.common.ResultListDataRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Util class that contains logic for pagination
 */

@Slf4j
@Component
public class PaginationUtil {

    /**
     * This method creates a page based on the passed pagination.
     *
     * First the pageStart and pageEnd are determined.
     * For example say if the list has 7 items and maxItems = 10 / skipCount = 1
     * The pageStart will be the first item on the second page -> 10
     * The pageEnd will be the last item on the second page -> 20
     *
     * @param list the list to retrieve a page from
     * @param pagination the pagination params
     * @return paginated ResultListDataRepresentation
     */
    public <T extends AbstractRepresentation> ResultListDataRepresentation<T> getPage(List<?> list, Pagination pagination) {
        // negative values for skipCount and maxItems aren't allowed
        if (pagination.getSkipCount() < 0 || pagination.getMaxItems() < 0) {
            throw new IllegalArgumentException(String.format("Found illegal values for skipCount [%d] or maxItems [%d]", pagination.getSkipCount(), pagination.getMaxItems()));
        }

        int total = list.size();
        int pageStart = pagination.getSkipCount();
        int pageEnd = pagination.getSkipCount() + pagination.getMaxItems(); // the last item on the page
        int size = getSize(pagination.getMaxItems(), total, pageEnd, pageStart);

          return new ResultListDataRepresentation<T>(size, total, pageStart, getPageList(list, pageStart, pageEnd));
    }

    /**
     * This method returns the sublist to add to the ResultListDataRepresentation. The sublist is determined by the pageStart.
     * If the pageStart exceeds the total amount of items in the list it will return an empty list, because there are no items on that page.
     * If the pageStart is less than the total amount of items in the list it will determine the list by the pageStart and the min value of total and pageEnd.
     *
     * @param list the list of which a sublist should be created
     * @param pageStart the start of the page
     * @param pageEnd the end of the page
     * @return List<T> the sublist
     */
    private <T> List<T> getPageList(List<?> list, int pageStart, int pageEnd) {
        return (pageStart < list.size()) ? (List<T>) list.subList(pageStart, Math.min(pageEnd, list.size())) : Collections.emptyList();
    }

    /**
     * This method retrieves the current page size.
     * (For example, say if total = 7, maxItems = 5 and skipCount = 1 will return size = 2.)
     *
     * @param maxItems the maxItems param
     * @param total the total amount of elements in the list
     * @param pageEnd the end of the page
     * @param pageStart the start of the page
     * @return int size
     */
    private int getSize(int maxItems, int total, int pageEnd, int pageStart) {
        if (pageStart > total) {
            return  0;
        } else if (pageEnd < total) {
            return maxItems;
        } else {
            return total - pageStart;
        }
    }
}
