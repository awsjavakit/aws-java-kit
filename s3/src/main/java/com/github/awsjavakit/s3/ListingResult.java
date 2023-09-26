package com.github.awsjavakit.s3;

import com.github.awsjavakit.misc.paths.UnixPath;
import java.util.ArrayList;
import java.util.List;

public class ListingResult {

    private final String listingStartingPoint;
    private final List<UnixPath> files;
    private final boolean truncated;

    public ListingResult(List<UnixPath> files, String listingStartingPoint, boolean isTruncated) {
        this.listingStartingPoint = listingStartingPoint;
        this.files = files;
        this.truncated = isTruncated;
    }

    public static ListingResult emptyResult() {
        return new ListingResult(new ArrayList<>(), null, false);
    }

    public String getListingStartingPoint() {
        return listingStartingPoint;
    }

    public List<UnixPath> getFiles() {
        return files;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public ListingResult add(ListingResult listFiles) {
        List<UnixPath> allFiles = new ArrayList<>(this.getFiles());
        allFiles.addAll(listFiles.getFiles());
        return new ListingResult(allFiles,
                                 listFiles.getListingStartingPoint(),
                                 listFiles.isTruncated());
    }
}
