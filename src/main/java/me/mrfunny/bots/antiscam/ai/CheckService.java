package me.mrfunny.bots.antiscam.ai;

import net.ricecode.similarity.*;

public class CheckService {
    public static double score(String string1, String string2){
        if(string1.equalsIgnoreCase(string2)){
            return 1.0;
        }
        JaroWinklerStrategy strategy1 = new JaroWinklerStrategy();
        StringSimilarityService service = new StringSimilarityServiceImpl(strategy1);
        double score1 = service.score(string1, string2);

        LevenshteinDistanceStrategy strategy2 = new LevenshteinDistanceStrategy();
        StringSimilarityService service2 = new StringSimilarityServiceImpl(strategy2);
        double score2 = service2.score(string1, string2);

        DiceCoefficientStrategy strategy3 = new DiceCoefficientStrategy();
        StringSimilarityService service3 = new StringSimilarityServiceImpl(strategy3);
        double score3 = service3.score(string1, string2);
        return (score1 + score2 + score3) / 3;
    }
}
