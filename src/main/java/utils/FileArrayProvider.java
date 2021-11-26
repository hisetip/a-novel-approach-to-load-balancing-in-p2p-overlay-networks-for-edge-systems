package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileArrayProvider {

    public static Integer[] readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<Integer> lines = new ArrayList<Integer>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(Integer.parseInt(line));
        }
        bufferedReader.close();
        return lines.toArray(new Integer[lines.size()]);
    }
}
