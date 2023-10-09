package jadex.idgenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *  This generator generates alliteration based composed names based on word lists.
 *  Needs 4 lists of adjectives with min 256 words each.
 *  Needs 4 nouns lists with min 1024 words each.
 */
public class IdGenerator 
{	
	/** Adjectives for auto-generated local IDs */
	private final String[] adjectives1;
	
	/** Adjectives for auto-generated local IDs */
	private final String[] adjectives2;
	
	/** Adjectives for auto-generated local IDs */
	private final String[] adjectives3;
	
	/** Adjectives for auto-generated local IDs */
	private final String[] adjectives4;
	
	/** Nouns for auto-generated local IDs */
	private final String[] nouns_a;
	
	/** Nouns for auto-generated local IDs */
	private final String[] nouns_b;
	
	/** Nouns for auto-generated local IDs */
	private final String[] nouns_d;
	
	/** Nouns for auto-generated local IDs */
	private final String[] nouns_f;
	
	/**
	 *  Create a new random generator.
	 */
	public IdGenerator()
	{
		this(false);
	}
	
	/**
	 *  Create a new generator.
	 */
	public IdGenerator(boolean deterministic)
	{	
		long seed = 208612059;
		Random r = deterministic? new Random(seed): new Random();
		List<String> tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1024)));
		Collections.shuffle(tmplist, r); // cannot be shuffled when ranges are used for starting letter determination
		adjectives1 = tmplist.toArray(new String[1024]);
		
		//seed = 265266541;
		//r = new Random(seed);
		tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1023)));
		Collections.shuffle(tmplist, r);
		tmplist.add(0, "");
		adjectives2 = tmplist.toArray(new String[1024]);
		
		//seed = 786761336;
		//r = new Random(seed);
		tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1023)));
		Collections.shuffle(tmplist, r);
		tmplist.add(0, "");
		adjectives3 = tmplist.toArray(new String[1024]);
		
		//seed = 384957210;
		//r = new Random(seed);
		tmplist = new ArrayList<>(Arrays.asList(Arrays.copyOf(Words.ADJECTIVES, 1023)));
		Collections.shuffle(tmplist, r);
		tmplist.add(0, "");
		adjectives4 = tmplist.toArray(new String[1024]);
		
		//seed = 292305523;
		//r = new Random(seed);
		tmplist = Arrays.asList(Arrays.copyOf(Words.NOUNS_A, 1024));
		Collections.shuffle(tmplist, r);
		nouns_a = tmplist.toArray(new String[1024]);
		
		tmplist = Arrays.asList(Arrays.copyOf(Words.NOUNS_B, 1024));
		Collections.shuffle(tmplist, r);
		nouns_b = tmplist.toArray(new String[1024]);
		
		tmplist = Arrays.asList(Arrays.copyOf(Words.NOUNS_D, 1024));
		Collections.shuffle(tmplist, r);
		nouns_d = tmplist.toArray(new String[1024]);
		
		tmplist = Arrays.asList(Arrays.copyOf(Words.NOUNS_F, 1024));
		Collections.shuffle(tmplist, r);
		nouns_f = tmplist.toArray(new String[1024]);
	}
	
	/**
	 *  Generate a String id from a number, used for auto-generating.
	 *  
	 *  @param num A number.
	 *  @return A String ID.
	 */
	public final String idStringFromNumber(long num)
	{
		int numval = (int) (num >>> 20 & 0xFFFL);
		//numval = numval * 4253 & 0xFFF; // 4253 is prime
		
		long low20val = num & 0xFFFFFL;
		low20val = low20val * 1049639L & 0xFFFFFL; // 1049639 is prime
		
		int selector = (int) ((low20val >>> 10) & 0x3FFL);
		
		String adj1 = adjectives1[selector];
		
		int nounnum = (int) (low20val & 0x3FFL);
		String noun = null;
		
		if(adj1.startsWith("A"))
			noun = nouns_a[nounnum];
		else if(adj1.startsWith("B"))
			noun = nouns_b[nounnum];
		else if(adj1.startsWith("D"))
			noun = nouns_d[nounnum];
		else if(adj1.startsWith("F"))
			noun = nouns_f[nounnum];
		/*if(selector<256)
			noun = nouns_a[nounnum];
		else if(selector<512)
			noun = nouns_b[nounnum];
		else if(selector<768)
			noun = nouns_d[nounnum];
		else
			noun = nouns_f[nounnum];*/
		
		String ret = String.join("", adjectives4[(int) ((num >>> 52) & 0x3FFL)],
									 adjectives3[(int) ((num >>> 42) & 0x3FFL)],
									 adjectives2[(int) ((num >>> 32) & 0x3FFL)],
									 adj1,
									 noun,
									 numval > 0? ("_" + Integer.toHexString(numval)) : "");
		return ret;
	}
	
	public static void main(String[] args) 
	{
		//IdGenerator gen = new IdGenerator(true);
		IdGenerator gen = new IdGenerator();
		
		for(int i=0; i<10000; i++)
		{
			System.out.println(i+": "+gen.idStringFromNumber(i));
		}
	}
}
