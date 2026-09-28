package hardcorequesting.common.io;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface DataReader {
    Optional<String> read(String name);
    
    default Stream<String> readAll(DirectoryStream.Filter<Path> filter) {
        return Stream.empty();
    }

    // Returns the text of all JSON files in the provided folder
    default Map<String, String> readFolder(String folder) {
        return Map.of();
    }
}
