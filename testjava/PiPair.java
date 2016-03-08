import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;

public class PiPair {
	
	/* record function(s) called by it(their) caller(s) as value in HashMap pipair, 
	 * use HashSet to eliminate duplicate functions */
	public static HashSet<String> stringset = new HashSet<String>();
	
	/* record function(s) called by it(their) caller(s) as one value-key set */
	public static HashMap<HashSet<String>, HashSet<String>> pipair = new HashMap<HashSet<String>, HashSet<String>>();
	
	public static void main(String [] args) throws Exception{
		
		int support = 3;
		float confidence = 65;
		
		int args_length = args.length;
		if(args_length >= 1){
			support = Integer.parseInt(args[0]);
			if(args_length == 2){
				confidence = Float.parseFloat(args[1])/100;
			} else if(args_length > 2){
				System.err.println("Error: Wrong parameter input.");
			}
		}
		
		System.out.println("support = " + support);
		System.out.println("confidence = " + confidence);
		
		readFile();
		findBug(support, confidence);
		
		return;
	}

	static void readFile() throws IOException{
		
		Scanner scanner = new Scanner(System.in);
		
		// test 1 input data:
		String note = "Call graph node <<null function>><<0x12f5f210>>  #uses=0\n"
				+ "CS<0x0> calls function 'main'\n"
				+ "CS<0x0> calls function 'printf'\n"
				+ "CS<0x0> calls function 'A'\n"
				+ "\n"
				+ "Call graph node for function: 'main'<<0x12f63f80>>  #uses=1\n"
				+ "CS<0x12f61058> calls function 'A'\n"
				+ "CS<0x12f5efb0> calls function 'printf'\n"
				+ "\n"
				+ "Call graph node for function: 'printf'<<0x12f62f50>>  #uses=4\n"
				+ "CS<0x0> calls external node\n"
				+ "\n"
				+ "Call graph node for function: 'A'<<0x12f62eb0>>  #uses=2\n"
				+ "CS<0x12f67d70> calls function 'printf'\n"
				+ "CS<0x12f67e10> calls function 'printf'\n";
		
		//Scanner scanner = new Scanner(note);
		
		// test 2:
		//File file = new File("C:" + File.separator + "input.txt");
		//Scanner scanner = new Scanner(file);
		
		String pattern_caller = "Call.*'(.*)'.*";
		Pattern p_caller = Pattern.compile(pattern_caller);
		Matcher m_caller = null;
		
		String pattern_callee = ".*calls.*'(.*)'.*";
		Pattern p_callee = Pattern.compile(pattern_callee);
		Matcher m_callee = null;
		
		String line = null; 
		String add_value = null;
		String add_key = null;
		
		while(scanner.hasNext()){
			line = scanner.nextLine();
			m_caller = p_caller.matcher(line);
			if(m_caller.matches()){
				add_value = m_caller.group(1);
				stringset.clear();
				while(scanner.hasNext()){
					line = scanner.nextLine();
					if(line.length() == 0){
						break;
					}
					
					m_callee = p_callee.matcher(line);
					if(m_callee.matches()){
						add_key = m_callee.group(1);
						if(!stringset.contains(add_key)){
							addToMap(add_key, add_value);
							stringset.add(add_key);
						}
					}
				}
			}
		}
		//System.out.println(pipair);
		
		scanner.close();
		return;
	}
	
	static void addToMap(String add_key, String add_value){
		
		HashSet<String> map_value = null;
		HashSet<String> map_key = new HashSet<String>();
 		map_key.add(add_key);
		
 		/* map_key only has one element */
 		if(pipair.containsKey(map_key)){
			map_value = pipair.get(map_key);
		} else{
			map_value = new HashSet<String>();
		}
		map_value.add(add_value);
		
		pipair.put(map_key, map_value);
		
		/* map_key has two elements */
		for(String key:stringset){

			map_key = new HashSet<String>();
			map_key.add(add_key);
			map_key.add(key);
			if(pipair.containsKey(map_key)){
				map_value = pipair.get(map_key);
			} else{
				map_value = new HashSet<String>();
			}
			map_value.add(add_value);
			
			pipair.put(map_key, map_value);
		}
		
		return;
	}
	
	static void findBug(int support, float confidence){
		
		/* value is the number of elements in pipair's value, 
		 * two HashMap separate pipairs whose keys include one or two elements*/
		
		HashMap<HashSet<String>, Integer> pipair_single = new HashMap<HashSet<String>, Integer>();
		HashMap<HashSet<String>, Integer> pipair_pair = new HashMap<HashSet<String>, Integer>();
		
		//Iterator<E> entries = pipair.entrySet().iterator();
		//while(entries.hasNext())
			
		for(HashMap.Entry<HashSet<String>, HashSet<String>> entry : pipair.entrySet()){
			HashSet<String> key = entry.getKey();
			HashSet<String> value = entry.getValue();
			int key_count = key.size();
			int value_count = value.size();
			if(key_count == 1){
				pipair_single.put(key, value_count);
			} else if(key_count == 2){
				pipair_pair.put(key, value_count);
			} else{
				System.err.println("Error");  ////// throw Exception
			}
		}
		
		//System.out.println("pipair_single: " + pipair_single.toString());
		//System.out.println("pipair_pair: " + pipair_pair.toString());
		
		for(HashMap.Entry<HashSet<String>, Integer> entry_single : pipair_single.entrySet()){
			int value_single = entry_single.getValue();
			HashSet<String> key_s = entry_single.getKey();
			String key_single = key_s.toString();
			key_single = key_single.replaceAll("\\[", "");
			key_single = key_single.replaceAll("\\]", "");
			
			if(value_single >= support){
				for(HashMap.Entry<HashSet<String>, Integer> entry_pair : pipair_pair.entrySet()){
					int value_pair = entry_pair.getValue();
					HashSet<String> key_pair = entry_pair.getKey();
					float confi = (float)value_pair / value_single;
					
					if(value_pair >= support && key_pair.contains(key_single) && confi > confidence && confi < 1){		
						HashSet<String> single = null;
						HashSet<String> pair = null;
						single = pipair.get(key_s);  // not key_single, it is a String not HashSet
						pair = pipair.get(key_pair);
						for(String s:single){
							if(!pair.contains(s)){
								/* printing bug */
								String bug_pair = key_pair.toString();
								bug_pair = bug_pair.replaceAll("\\[", "(");
								bug_pair = bug_pair.replaceAll("\\]", ")");
								System.out.println("bug: " + key_single + " in "+ s 
									+ ", pair: " + bug_pair
									+ ", support: " + Integer.toString(value_pair)
									+ ", confidence: " + NumberFormat.getPercentInstance().format(confi)
									);
							}	
						}
					}
				}
			}
	
		} // end of for loop 
		
	} // end of function findBug
	
} // end of class PiPair
