package me.mrfunny.bots.antiscam.ai;

import net.ricecode.similarity.*;

public class CheckService {

    private static String[] blockedDomains = {"cs-riptide"};

    public static double score(String string1, String string2){
        for(String blockedDomain : blockedDomains){
            if(string2.contains(blockedDomain)){return 0.99;}
        }
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

    private final static SimilarityStrategy[] strategies = {new JaroWinklerStrategy(), new LevenshteinDistanceStrategy(), new DiceCoefficientStrategy()};

    public static double scoreV2(String string1, String string2){
        if(string1.equalsIgnoreCase(string2)){
            return 1.0;
        }
        double sum = 0;
        for(SimilarityStrategy strategy : strategies){
            StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
            sum += service.score(string1, string2);
        }
        return sum / strategies.length;
    }
}
