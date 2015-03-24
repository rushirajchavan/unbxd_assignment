package unbxd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import edu.cmu.lti.jawjaw.util.Configuration;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

import net.sf.extjwnl.JWNLException;

import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;

public class NGrams {
	private Dictionary dictionary;

	public NGrams() throws JWNLException {
		dictionary = Dictionary.getDefaultResourceInstance();
	}

	private boolean contains(String word) throws JWNLException {
		boolean exist = false;
		for(POS pos: POS.getAllPOS()) {
			exist = exist || dictionary.getIndexWord(pos, word) != null;
		}
		return exist;
	}

	public Dictionary getDictionary() {
		return dictionary;
	}

	public void setDictionary(Dictionary dictionary) {
		this.dictionary = dictionary;
	}

	void segmentString(String word, ArrayList<String> list, ArrayList<ArrayList<String>> listOfList) throws JWNLException {
		int len = word.length();
		for(int i = 1; i <= len; i++) {
			String prefix = word.substring(0, i);
			if(contains(prefix) && prefix.length() > 2) {
				if(i == len) {
					list.add(prefix);
					listOfList.add(list);
					return;
				}
				ArrayList<String> newList = new ArrayList<String>(list);
				newList.add(prefix);
				segmentString(word.substring(i, len), newList, listOfList);
			}
		}
	}

	public static void main(String args[]) throws JWNLException, IOException {
		NGrams ngram = new NGrams();

		POSTaggerME tagger = new POSTaggerME(new POSModel(NGrams.class.getClassLoader().getResourceAsStream("en-pos-maxent.bin")));
		InputStream is = NGrams.class.getClassLoader().getResourceAsStream("words.txt");

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String word;
		ArrayList<ArrayList<String>> listOfList;

		WS4JConfiguration.getInstance().setLeskNormalize( false );
		WS4JConfiguration.getInstance().setMFS( false );
		Configuration.getInstance().setMemoryDB(false);

		NictWordNet nict = new NictWordNet();

		WuPalmer wu = new WuPalmer(nict);
		Resnik r = new Resnik(nict);

		Dictionary d = ngram.getDictionary();

		while((word = reader.readLine()) != null) {
			String s[] = {word};
			String tags[] = tagger.tag(s);
			listOfList = new ArrayList<ArrayList<String>>();
			ngram.segmentString(word, new ArrayList<String>(), listOfList);
			double maxScore = Float.MIN_VALUE, temp;
			ArrayList<String> maxList = null;
			Double max = 0.0;
			for(ArrayList<String> list: listOfList) {
				temp = 0.0;
				max = 0.0;
				List<String> list1 = tagger.tag(list);
				int cnt = 0;
				for(String str: list) {
					if(!str.equals(word) && tags[0].equals(list1.get(cnt))) {
						temp = wu.calcRelatednessOfWords(word, str);
						if (temp > max)	
							max = temp;
					}
					cnt++;
				}
				if(Double.compare(max, maxScore) > 0) {
					maxScore = max;
					maxList = list;
				}
			}

			System.out.print(word + " - " );

			Double minInterScore = Double.MAX_VALUE;
			Double temp1;

			int cnt2 = 0;
			if(maxList!=null) {
				while((cnt2+1) < maxList.size()) {
					temp1 = r.calcRelatednessOfWords(maxList.get(cnt2), maxList.get(cnt2+1));
					if(Double.compare(minInterScore, temp1) > 0) {
						minInterScore = temp1;
					}
					cnt2++;
				}
			}

			if(maxList == null || Double.compare(minInterScore, 0.0) < 0  || Double.compare(maxScore, 0.0) < 0  ) {
				System.out.println("NA");
			}
			else {
				for(String str: maxList) {
					System.out.print(str + " ");
				}
				System.out.println();
			}
		}
	}
}