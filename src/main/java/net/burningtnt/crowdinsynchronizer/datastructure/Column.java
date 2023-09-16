package net.burningtnt.crowdinsynchronizer.datastructure;

import net.burningtnt.crowdinsynchronizer.crowdin.objects.CrowdinFileObject;
import net.burningtnt.crowdinsynchronizer.crowdin.objects.CrowdinTranslationKeyItemObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Column {
    private final CrowdinFileObject crowdinFile;

    private final Map<String, CrowdinTranslationKeyItemObject> crowdinTranslationKeys = new ConcurrentSkipListMap<>();

    private final Set<String> localTranslationKeys = new ConcurrentSkipListSet<>();

    private final List<Difference> concurrentDifferences = new LinkedList<>();

    private final List<Difference> blockedDifferences = new LinkedList<>();

    public Column(CrowdinFileObject crowdinFile) {
        this.crowdinFile = crowdinFile;
    }

    public CrowdinFileObject getCrowdinFile() {
        return this.crowdinFile;
    }

    public Map<String, CrowdinTranslationKeyItemObject> getCrowdinTranslationKeys() {
        return this.crowdinTranslationKeys;
    }

    public Set<String> getLocalTranslationKeys() {
        return this.localTranslationKeys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Column column = (Column) o;

        return crowdinFile.equals(column.crowdinFile);
    }

    @Override
    public int hashCode() {
        return crowdinFile.hashCode();
    }

    public void pushDifference(DifferenceType differenceType, String sourceTranslationkey) {
        (differenceType.isAllowConcurrency() ? this.concurrentDifferences : this.blockedDifferences).add(new Difference(differenceType, sourceTranslationkey));
    }

    public List<Difference> getConcurrentDifferences() {
        return this.concurrentDifferences;
    }

    public List<Difference> getBlockedDifferences() {
        return this.blockedDifferences;
    }
}
