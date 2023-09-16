package net.burningtnt.crowdinsynchronizer.crowdin;

import net.burningtnt.crowdinsynchronizer.crowdin.objects.DataItem;
import net.burningtnt.crowdinsynchronizer.utils.Lang;
import net.burningtnt.crowdinsynchronizer.utils.io.ExceptionalFunction;
import net.burningtnt.crowdinsynchronizer.utils.io.NetIterator;
import net.burningtnt.crowdinsynchronizer.utils.io.NetworkUtils;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CrowdinPageIterator<E> implements NetIterator<E> {
    public static URI format(String url, Map<String, String> queryArgs, PageDataContainer.PageData pageData) throws IOException {
        return NetworkUtils.format(
                url,
                Stream.concat(
                        queryArgs.entrySet().stream(),
                        Stream.of(Map.entry("offset", String.valueOf(pageData.offset)))
                ));
    }

    public static final class PageDataContainer {
        public static final class PageData {
            private final int offset;
            private final int limit;

            private PageData(int offset, int limit) {
                this.offset = offset;
                this.limit = limit;
            }
        }

        private final DataItem[] data;

        private final PageData pagination;

        private PageDataContainer(DataItem[] data, PageData pagination) {
            this.data = data;
            this.pagination = pagination;
        }
    }

    private final ExceptionalFunction<PageDataContainer.PageData, HttpUriRequest, IOException> requester;

    private final Class<E> dataType;

    private final Supplier<CrowdinAPI.HttpClientDelegate> httpClientDelegateSupplier;

    private int currentOffset = 0;

    private int responseOffset = -1;

    private List<E> responses = null;

    private PageDataContainer.PageData pageData = null;

    public CrowdinPageIterator(ExceptionalFunction<PageDataContainer.PageData, HttpUriRequest, IOException> requester, Supplier<CrowdinAPI.HttpClientDelegate> httpClientDelegateSupplier, Class<E> dataType) {
        this.requester = requester;
        this.httpClientDelegateSupplier = httpClientDelegateSupplier;
        this.dataType = dataType;
    }

    @Override
    public boolean hasNext() throws IOException {
        request();
        return this.responses.size() > 0;
    }

    @Override
    public List<E> next() throws NoSuchElementException, IOException {
        request();
        if (this.responses.size() == 0) {
            throw new NoSuchElementException();
        }
        this.currentOffset += this.pageData.limit;
        return this.responses;
    }

    @Override
    public int aboutSize() throws IOException {
        request();
        return this.pageData.offset + this.responses.size();
    }

    private void request() throws IOException {
        if (this.responses != null && this.currentOffset == this.responseOffset) {
            return;
        }

        HttpUriRequest request = this.requester.apply(new PageDataContainer.PageData(this.currentOffset, 50));
        PageDataContainer pageDataContainer = this.httpClientDelegateSupplier.get().execute(
                request,
                response -> Lang.getGson().fromJson(
                        Lang.readAllBytesAsString(() -> NetworkUtils.readResponseBody(response.getEntity())),
                        PageDataContainer.class
                )
        );
        this.responses = Arrays.stream(pageDataContainer.data).map(
                dataItem -> Lang.getGson().fromJson(dataItem.getData(), this.dataType)
        ).collect(Collectors.toList());
        this.responseOffset = this.currentOffset;
        this.pageData = pageDataContainer.pagination;
    }
}
