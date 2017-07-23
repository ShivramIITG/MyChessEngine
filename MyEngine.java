import java.util.*;
import javafx.util.Pair;
import javax.swing.*;
import java.util.concurrent.ThreadLocalRandom;

/*
Move: sliding-pieces,Pawn promotion, en-passant,castling
Algo: alpha-beta,rating,sorting OR trans-table+zoborist
Bugs: cant move to 63
Opts: see his code(iterate thru 1s),Remove unnecessary 1<<i's,De Bruijn Multiplication
*/

class zoboristTable {
	public long zoboristKey;
	public int depth;
	public int flag;
	public String eval;
	public int player;
	public int valid;
}
public class MyEngine{
	static long whitePieces = 	0b0000000000000000000000000000000000000000000000001111111111111111L;
	static long whitePawns = 	0b0000000000000000000000000000000000000000000000001111111100000000L;
	static int whiteKnight1pos = 1,whiteKnight2pos =  6,whiteKingpos = 3;
	static int whiteQueenpos = 4,whiteRook1pos = 0,whiteRook2pos = 7,whiteBishop1pos = 2,whiteBishop2pos = 5;
	
	//static Pair<String,int> whitePromoted;
	static int whitePromotedCount = -1;
	
	static long blackPieces = 	0b1111111111111111000000000000000000000000000000000000000000000000L;
	static long blackPawns = 	0b0000000011111111000000000000000000000000000000000000000000000000L;
	static int blackKnight1pos = 62,blackKnight2pos =  57,blackKingpos = 59;
	static int blackQueenpos = 60,blackRook1pos = 56,blackRook2pos = 63,blackBishop1pos = 58,blackBishop2pos = 61;
	
	//static Pair<String,int> blackPromoted;
	static int blackPromotedCount = -1;
	
	static long[] knightMask,kingMask,blackPawnMask,whitePawnMask;
	
	static int zoboristSize = 1048583;
	static long[][] zoboristValue;
	static long zoboristHash;
	static zoboristTable[] hashTable;
	
	static long RankMasks[] =
    {
        0xFFL, 0xFF00L, 0xFF0000L, 0xFF000000L, 0xFF00000000L, 0xFF0000000000L, 0xFF000000000000L, 0xFF00000000000000L
    };
    static long FileMasks[] =
    {
        0x101010101010101L, 0x202020202020202L, 0x404040404040404L, 0x808080808080808L,
        0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
    };
    static long DiagonalMasks[] =
    {
	0x1L, 0x102L, 0x10204L, 0x1020408L, 0x102040810L, 0x10204081020L, 0x1020408102040L,
	0x102040810204080L, 0x204081020408000L, 0x408102040800000L, 0x810204080000000L,
	0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L
    };
    static long AntiDiagonalMasks[] =
    {
	0x80L, 0x8040L, 0x804020L, 0x80402010L, 0x8040201008L, 0x804020100804L, 0x80402010080402L,
	0x8040201008040201L, 0x4020100804020100L, 0x2010080402010000L, 0x1008040201000000L,
	0x804020100000000L, 0x402010000000000L, 0x201000000000000L, 0x100000000000000L
    };
	
	public static long DiagandAntidiagMoves(int position){
		long allPieces = blackPieces|whitePieces;
		long curPiece = 1L<<position;
		
		long occDiag = allPieces&DiagonalMasks[position/8 + position%8];
		long DiagrtAttack = occDiag^(occDiag-2*curPiece);
		DiagrtAttack&=DiagonalMasks[position/8 + position%8];
		long DiagltAttack = Long.reverse( (Long.reverse(occDiag) - 2*Long.reverse(curPiece))^Long.reverse(occDiag));
		DiagltAttack&=DiagonalMasks[position/8 + position%8];
		
		
		long occAntiDiag = allPieces&AntiDiagonalMasks[position/8 + 7 - position%8];
		long AntiDiagRtAttack = occAntiDiag^(occAntiDiag-2*curPiece);
		AntiDiagRtAttack&=AntiDiagonalMasks[position/8 + 7 - position%8];
		long AntiDiagLtAttack = Long.reverse( (Long.reverse(occAntiDiag) - 2*Long.reverse(curPiece))^Long.reverse(occAntiDiag));
		AntiDiagLtAttack&=AntiDiagonalMasks[position/8 + 7 - position%8];
		
		return DiagrtAttack|DiagltAttack|AntiDiagLtAttack|AntiDiagRtAttack;
	}
	
	
	public static long HorizandVertMoves(int position){
		long allPieces = blackPieces|whitePieces;
		long curPiece = 1L<<position;
		
		long occHoriz = allPieces&RankMasks[position/8];
		long ltAttack = occHoriz^(occHoriz-2*curPiece);
		ltAttack&=RankMasks[position/8];
		long rtAttack = Long.reverse( (Long.reverse(occHoriz) - 2*Long.reverse(curPiece))^Long.reverse(occHoriz));
		rtAttack&=RankMasks[position/8];
		
		
		long occVert = allPieces&FileMasks[position%8];
		long upAttack = occVert^(occVert-2*curPiece);
		upAttack&=FileMasks[position%8];
		long downAttack = Long.reverse( (Long.reverse(occVert) - 2*Long.reverse(curPiece))^Long.reverse(occVert));
		downAttack&=FileMasks[position%8];
		
		return upAttack|downAttack|ltAttack|rtAttack;
	}
	
	
	public static void initializeMasks(){
		knightMask = new long[64];
		kingMask = new long[64];
		blackPawnMask = new long[64];
		whitePawnMask = new long[64];
		for(int i=0;i<=63;++i){
			long pos=1L<<i;
			
			knightMask[i]=0L;
			knightMask[i] |=  ( (i%8 == 7) ? 0L:pos<<17 ) ;
			knightMask[i] |=  ( (i%8 == 0) ? 0L:pos>>>17 ) ;
			knightMask[i] |=  ( (i%8 == 0) ? 0L:pos<<15 ) ;
			knightMask[i] |=  ( (i%8 == 7) ? 0L:pos>>>15 ) ;
			knightMask[i] |=  ( (i%8 >= 6) ? 0L:pos<<10 ) ;
			knightMask[i] |=  ( (i%8 <= 1) ? 0L:pos>>>10 ) ;
			knightMask[i] |=  ( (i%8 <= 1) ? 0L: pos<<6 ) ;
			knightMask[i] |=  ( (i%8 >= 6) ? 0L:pos>>>6 ) ;
			
			
			kingMask[i]=(pos<<8)|(pos>>>8);
			kingMask[i] |=  ( (i%8 == 0) ? 0L:(pos<<7 | pos>>>1|pos>>>9) ) ;
			kingMask[i] |=  ( (i%8 == 7) ? 0L:(pos>>>7 | pos<<1|pos<<9) ) ;
			
			blackPawnMask[i]=0L;
			blackPawnMask[i] |=  ( (i%8 == 0) ? 0L:pos>>>9 ) ;
			blackPawnMask[i] |=  ( (i%8 == 7) ? 0L:pos>>>7 ) ;
			
			
			whitePawnMask[i]=0L;
			whitePawnMask[i] |= ( (i%8 == 0) ? 0L:pos<<7 ) ;
			whitePawnMask[i] |=  ( (i%8 == 7) ? 0L:pos<<9 ) ;
			
		}
		zoboristValue = new long[12][64];
		for(int i=0;i<12;++i) for(int j=0;j<64;++j) zoboristValue[i][j]=ThreadLocalRandom.current().nextLong((1L<<62));
		
		zoboristHash = 0L;
		zoboristHash ^= zoboristValue[0][blackKingpos];
		zoboristHash ^= zoboristValue[1][blackQueenpos];
		zoboristHash ^= (zoboristValue[2][blackRook1pos]^zoboristValue[2][blackRook2pos]);
		zoboristHash ^= (zoboristValue[3][blackKnight1pos]^zoboristValue[3][blackKnight2pos]);
		zoboristHash ^= (zoboristValue[4][blackBishop1pos]^zoboristValue[4][blackBishop2pos]);
		for(int i=55;i>=48;i--) zoboristHash ^= zoboristValue[5][i];
		
		
		zoboristHash ^= zoboristValue[6][whiteKingpos];
		zoboristHash ^= zoboristValue[7][whiteQueenpos];
		zoboristHash ^= (zoboristValue[8][whiteRook1pos]^zoboristValue[2][whiteRook2pos]);
		zoboristHash ^= (zoboristValue[9][whiteKnight1pos]^zoboristValue[3][whiteKnight2pos]);
		zoboristHash ^= (zoboristValue[10][whiteBishop1pos]^zoboristValue[4][whiteBishop2pos]);
		for(int i=15;i>=8;i--) zoboristHash ^= zoboristValue[11][i];
		
		hashTable = new zoboristTable[zoboristSize];
		for(int i=0;i<zoboristSize;++i){
			hashTable[i]= new zoboristTable();
			hashTable[i].valid = 0;
		}
	}
	
	
	
	public static void debugTests(){
		/*
		blackPieces = 0L;
		System.out.println(Long.toBinaryString(whitePawnForward(blackPawns)));*/
		
		long PAWN_MOVES = 0b0000000000000001000000000000000000000000000000001011110101001010L;
		long possibility=PAWN_MOVES&~(PAWN_MOVES-1);
        while (possibility != 0)
        {
            int index=Long.numberOfTrailingZeros(possibility);
			//System.out.println(index);
            PAWN_MOVES&=~possibility;
            possibility=PAWN_MOVES&~(PAWN_MOVES-1);
        }
	}
	public static void main(String[] args){
		debugTests();
		
		initializeMasks();
		

		String blackMovesAllowed = blackCheckMoves();
		//System.out.println(blackMovesAllowed);
		
		JFrame f=new JFrame("Chess Tutorial");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MyEngineUI ui=new MyEngineUI();
        f.add(ui);
        f.setSize(500, 500);
        f.setVisible(true);
		
		
	}
	
	public static void blackMakeMove(int st,int en,char piece,char atkPiece){
		
		if(atkPiece != ' ') {
			whitePieces ^= (1L<<en);
			if(atkPiece == 'P') {
				whitePawns ^= (1L<<en);
				zoboristHash ^= zoboristValue[11][en];
			}
			else if(atkPiece == 'K') {whiteKnight1pos = -1;zoboristHash ^= zoboristValue[9][en];}
			else if(atkPiece == 'k') {whiteKnight2pos = -1;zoboristHash ^= zoboristValue[9][en];}
			else if(atkPiece == 'B') {whiteBishop1pos = -1;zoboristHash ^= zoboristValue[10][en];}
			else if(atkPiece == 'b') {whiteBishop2pos = -1;zoboristHash ^= zoboristValue[10][en];}
			else if(atkPiece == 'R') {whiteRook1pos = -1;zoboristHash ^= zoboristValue[8][en];}
			else if(atkPiece == 'r') {whiteRook2pos = -1;zoboristHash ^= zoboristValue[8][en];}
			else {whiteQueenpos = -1;zoboristHash ^= zoboristValue[7][en];}
		}
		
		blackPieces ^= (1L<<st);
		blackPieces |= (1L<<en);
		if(piece == 'P'){
			blackPawns ^= (1L<<st);
			blackPawns |= (1L<<en);
			zoboristHash ^= zoboristValue[5][st];
			zoboristHash ^= zoboristValue[5][en];
		}
		else if(piece == 'A') {blackKingpos = en;zoboristHash ^= zoboristValue[0][st];
			zoboristHash ^= zoboristValue[0][en];}
			
		else if(piece == 'K') {blackKnight1pos = en;zoboristHash ^= zoboristValue[3][st];
			zoboristHash ^= zoboristValue[3][en];}
		else if(piece == 'k') {blackKnight2pos = en;zoboristHash ^= zoboristValue[3][st];
			zoboristHash ^= zoboristValue[3][en];}
			
		else if(piece == 'B') {blackBishop1pos = en;zoboristHash ^= zoboristValue[4][st];
			zoboristHash ^= zoboristValue[4][en];}
		else if(piece == 'b') {blackBishop2pos = en;zoboristHash ^= zoboristValue[4][st];
			zoboristHash ^= zoboristValue[4][en];}
			
		else if(piece == 'R') {blackRook1pos = en;zoboristHash ^= zoboristValue[2][st];
			zoboristHash ^= zoboristValue[2][en];}
		else if(piece == 'r') {blackRook2pos = en;zoboristHash ^= zoboristValue[2][st];
			zoboristHash ^= zoboristValue[2][en];}
			
		else {blackQueenpos = en;zoboristHash ^= zoboristValue[1][st];
			zoboristHash ^= zoboristValue[1][en];}
			
	}
	
	public static void blackUndoMove(int st,int en,char piece,char atkPiece){
		
		if(atkPiece != ' ') {
			whitePieces |= (1L<<en);
			if(atkPiece == 'P') {
				whitePawns |= (1L<<en);
				zoboristHash ^= zoboristValue[11][en];
			}
			else if(atkPiece == 'K') {whiteKnight1pos = en;zoboristHash ^= zoboristValue[9][en];}
			else if(atkPiece == 'k') {whiteKnight2pos = en;zoboristHash ^= zoboristValue[9][en];}
			else if(atkPiece == 'B') {whiteBishop1pos = en;zoboristHash ^= zoboristValue[10][en];}
			else if(atkPiece == 'b') {whiteBishop2pos = en;zoboristHash ^= zoboristValue[10][en];}
			else if(atkPiece == 'R') {whiteRook1pos = en;zoboristHash ^= zoboristValue[8][en];}
			else if(atkPiece == 'r') {whiteRook2pos = en;zoboristHash ^= zoboristValue[8][en];}
			else {whiteQueenpos = en;zoboristHash ^= zoboristValue[7][en];}
		}
		
		blackPieces |= (1L<<st);
		blackPieces ^= (1L<<en);
		if(piece == 'P'){
			blackPawns |= (1L<<st);
			blackPawns ^= (1L<<en);
			zoboristHash ^= zoboristValue[5][st];
			zoboristHash ^= zoboristValue[5][en];
		}
		else if(piece == 'A') {blackKingpos = st;zoboristHash ^= zoboristValue[0][st];
			zoboristHash ^= zoboristValue[0][en];}
			
		else if(piece == 'K') {blackKnight1pos = st;zoboristHash ^= zoboristValue[3][st];
			zoboristHash ^= zoboristValue[3][en];}
		else if(piece == 'k') {blackKnight2pos = st;zoboristHash ^= zoboristValue[3][st];
			zoboristHash ^= zoboristValue[3][en];}
			
		else if(piece == 'B') {blackBishop1pos = st;zoboristHash ^= zoboristValue[4][st];
			zoboristHash ^= zoboristValue[4][en];}
		else if(piece == 'b') {blackBishop2pos = st;zoboristHash ^= zoboristValue[4][st];
			zoboristHash ^= zoboristValue[4][en];}
			
		else if(piece == 'R') {blackRook1pos = st;zoboristHash ^= zoboristValue[2][st];
			zoboristHash ^= zoboristValue[2][en];}
		else if(piece == 'r') {blackRook2pos = st;zoboristHash ^= zoboristValue[2][st];
			zoboristHash ^= zoboristValue[2][en];}
			
		else {blackQueenpos = st;zoboristHash ^= zoboristValue[1][st];
			zoboristHash ^= zoboristValue[1][en];}
			
	}
	
	
	public static String rearrangeMoves(String list,int player){
		
		int []score = new int[list.length()/6];
		for(int i=0;i<list.length();i+=6){
			int stind = (list.charAt(i+0)-'0')*10 + (list.charAt(i+1)-'0');
			int enind = (list.charAt(i+2)-'0')*10 + (list.charAt(i+3)-'0');
			if(player == 1){
				blackMakeMove(stind,enind,list.charAt(i+4),list.charAt(i+5));
				score[i/6] = MyRating.RateBlack(0,0);
				blackUndoMove(stind,enind,list.charAt(i+4),list.charAt(i+5));
			}
			else{
				whiteMakeMove(stind,enind,list.charAt(i+4),list.charAt(i+5));
				score[i/6] = -MyRating.RateBlack(0,0);
				whiteUndoMove(stind,enind,list.charAt(i+4),list.charAt(i+5));
			
			}
		}
		
		String newListA="", newListB=list;
        for (int i=0;i<Math.min(6, list.length()/6);i++) {
            int max=-1000000, maxLocation=0;
            for (int j=0;j<list.length()/6;j++) {
                if (score[j]>max) {max=score[j]; maxLocation=j;}
            }
            score[maxLocation]=-1000000;
            newListA+=list.substring(maxLocation*6,maxLocation*6+6);
            newListB=newListB.replace(list.substring(maxLocation*6,maxLocation*6+6), "");
        }
        return newListA+newListB;
	}
	public static void checkandStore(int hashKey,long zoboristHash,int depth,int hstype,String move,int player){
		if(hashTable[hashKey].valid == 0 || hashTable[hashKey].depth <=  depth){
			hashTable[hashKey].zoboristKey=zoboristHash;
			hashTable[hashKey].depth=depth;
			hashTable[hashKey].flag=hstype;
			hashTable[hashKey].eval=move;
			hashTable[hashKey].player=player;
			hashTable[hashKey].valid=1;
			return;
		}
	}
	
	public static String AlphaBeta(int alpha,int beta,int player,int depth){
		int hashKey = (int)(zoboristHash%zoboristSize);
		if(hashTable[hashKey].zoboristKey == zoboristHash && hashTable[hashKey].player == player && hashTable[hashKey].valid == 1 && hashTable[hashKey].depth >= depth){
			if(hashTable[hashKey].flag == 0) {
				System.out.println("ZOBORIST");
				return hashTable[hashKey].eval;
			}
			//beta cutoff
			else if(hashTable[hashKey].flag == 1){
				int bestScore=Integer.valueOf(hashTable[hashKey].eval.substring(6));
				if(bestScore >= beta) {
					System.out.println("ZOBORIST");
					return  hashTable[hashKey].eval;
				}
			}
			//alpha cutoff
			else{
				int bestScore=Integer.valueOf(hashTable[hashKey].eval.substring(6));
				if(bestScore <= alpha) {
					System.out.println("ZOBORIST");
					return  hashTable[hashKey].eval;
				}
			}
		}	
		
		String listcurMoves;
		if(player == 1) listcurMoves = blackCheckMoves();
		else listcurMoves = whiteCheckMoves();
		if(listcurMoves.length() == 0){
			if(player == 1) {
				if(blackKingSafe()) {
					checkandStore(hashKey,zoboristHash,depth,0,"$$$$$$"+-5000000*(depth+1),1);
					return "$$$$$$"+-5000000*(depth+1);
				}
				else {
					checkandStore(hashKey,zoboristHash,depth,0,"$$$$$$"+-10000000*(depth+1),1);
					return "$$$$$$"+-10000000*(depth+1);
				}
			}
			else {
				if(whiteKingSafe()) {
					checkandStore(hashKey,zoboristHash,depth,0,"$$$$$$"+-5000000*(depth+1),0);
					return "$$$$$$"+-5000000*(depth+1);
				}
				else {
					checkandStore(hashKey,zoboristHash,depth,0,"$$$$$$"+-10000000*(depth+1),0);
					return "$$$$$$"+10000000*(depth+1);
				}
			}
		}
		if(depth == 0) {
			String retS = "$$$$$$"+MyRating.RateBlack(depth,listcurMoves.length());
			checkandStore(hashKey,zoboristHash,depth,0,retS,player);
			return retS;
		}
		
		int myScore = alpha;
		if(player == 0) myScore=beta;
		String myBestMove="$$$$$$";
		
		listcurMoves = rearrangeMoves(listcurMoves,player);
		
		int hstype=0;
		/********************* SORT CUR MOVES(BEST MOVES FOR BOTH SIDES) ***************************/
		for(int i=0;i<listcurMoves.length();i+=6){
			int stind = (listcurMoves.charAt(i+0)-'0')*10 + (listcurMoves.charAt(i+1)-'0');
			int enind = (listcurMoves.charAt(i+2)-'0')*10 + (listcurMoves.charAt(i+3)-'0');
			if(player == 1){
				blackMakeMove(stind,enind,listcurMoves.charAt(i+4),listcurMoves.charAt(i+5));
				String replybyWhite = AlphaBeta(myScore,beta,0,depth-1);
				int retScore=Integer.valueOf(replybyWhite.substring(6));
				blackUndoMove(stind,enind,listcurMoves.charAt(i+4),listcurMoves.charAt(i+5));
				//myScore = Math.max(myScore,retScore);
				if(myScore < retScore){///NOTE: can be changed to <
					myScore = retScore;
					myBestMove = listcurMoves.substring(i,i+6);
				}
				//alpha = Math.max(alpha,retScore);
				if(myScore>=beta) {myScore=beta;hstype=1;break;}
			}
			else{
				whiteMakeMove(stind,enind,listcurMoves.charAt(i+4),listcurMoves.charAt(i+5));
				String replybyBlack = AlphaBeta(alpha,myScore,1,depth-1);
				int retScore=Integer.valueOf(replybyBlack.substring(6));
				whiteUndoMove(stind,enind,listcurMoves.charAt(i+4),listcurMoves.charAt(i+5));
				//myScore = Math.min(myScore,retScore);
				if(myScore > retScore){
					myScore = retScore;
					myBestMove = listcurMoves.substring(i,i+6);
				}
				//beta = Math.min(beta,retScore);
				if(alpha>=myScore) {myScore=alpha;hstype=2;break;}
			}
		}
		if(myBestMove == "$$$$$$") myBestMove = listcurMoves.substring(0,6); // just for safety
		checkandStore(hashKey,zoboristHash,depth,hstype,myBestMove+myScore,player);
		return myBestMove+myScore;
		
	}
	
	public static void blackPlaysMove(/*String possibilities*/){
		String bestMove = AlphaBeta(-1000000000,1000000000,1,4);
		//System.out.println("Alpha beta Returned " + bestMove);
		
		int stind = (bestMove.charAt(0)-'0')*10 + (bestMove.charAt(1)-'0');
		int enind = (bestMove.charAt(2)-'0')*10 + (bestMove.charAt(3)-'0');
		blackMakeMove(stind,enind,bestMove.charAt(4),bestMove.charAt(5));
	}
	
	
	public static char getWhiteAttacked(int pos){
		if(whiteKnight1pos == pos) return 'K';
		if(whiteKnight2pos == pos) return 'k';
		
		if(whiteBishop1pos == pos) return 'B';
		if(whiteBishop2pos == pos) return 'b';
		
		if(whiteRook1pos == pos) return 'R';
		if(whiteRook2pos == pos) return 'r';
		
		if(whiteQueenpos == pos) return 'Q';
		if(whiteKingpos == pos) return 'A';
		if((whitePawns & (1L<<pos) )>0L) return 'P';
		return ' ';
	}
	
	//takes care of king safe.
	public static String blackCheckMoves(){
		String ret = "";
		
		//pawns fwd
		long fwd = blackPawnForward(blackPawns);
		//System.out.println(Long.toBinaryString(fwd));
		for(int i=63;i>=0;i--) {
			if( (fwd & (1L<<i)) > 0L) {
				int off = 16;
				if( (blackPawns&(1L<<(i+8))) > 0L) off = 8;
			
				blackMakeMove(i+off,i,'P',' ');
				if(blackKingSafe()){
					blackUndoMove(i+off,i,'P',' ');
					//store
					if(i+off < 10) ret+='0';
					ret+=(i+off);
					if(i<10) ret+='0';
					ret+=i;
					ret+="P ";
				}
				else blackUndoMove(i+off,i,'P',' ');
			}
		}
		
		//pawns attack
		long atk = blackPawnAttacks(blackPawns);
		for(int i=63;i>=0;i--) {
			if( (atk & (1L<<i)) > 0L) {
				/*char atkPiece;
				if(whiteKnight1pos == i) atkPiece = 'K';
				else if(whiteKnight2pos == i) atkPiece = 'k';
				else atkPiece = 'P';*/
				char atkPiece = getWhiteAttacked(i);

				if(i%8!=0 && ((blackPawns&(1L<<(i+7)))>0L) ){
					blackMakeMove(i+7,i,'P',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(i+7,i,'P',atkPiece);
						if(i+7 < 10) ret+='0';
						ret+=(i+7);
						if(i<10) ret+='0';
						ret+=i;
						ret+='P';
						ret+=atkPiece;
					}
					else blackUndoMove(i+7,i,'P',atkPiece);
					
				}
				
				if(i%8!=7 && ((blackPawns&(1L<<(i+9))) > 0L)){
					
					blackMakeMove(i+9,i,'P',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(i+9,i,'P',atkPiece);
						if(i+9 < 10) ret+='0';
						ret+=(i+9);
						if(i<10) ret+='0';
						ret+=i;
						ret+='P';
						ret+=atkPiece;
					}
					else blackUndoMove(i+9,i,'P',atkPiece);
				}
			}
		}
		
		
		//knight1
		if(blackKnight1pos != -1){
			long blackKnight1posible = blackKnightMoves(blackKnight1pos);
			for(int i=63;i>=0;i--) {
				if( (blackKnight1posible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackKnight1pos;
					blackMakeMove(blackKnight1pos,i,'K',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'K',atkPiece);
						if(blackKnight1pos < 10) ret+='0';
						ret+=blackKnight1pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='K';
						ret+=atkPiece;
					}
					else blackUndoMove(tem,i,'K',atkPiece);
				}
			}
		}
		
		//knight2
		if(blackKnight2pos != -1){
			long blackKnight2posible = blackKnightMoves(blackKnight2pos);
			for(int i=63;i>=0;i--) {
				if( (blackKnight2posible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackKnight2pos;
					blackMakeMove(blackKnight2pos,i,'k',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'k',atkPiece);
						if(blackKnight2pos < 10) ret+='0';
						ret+=blackKnight2pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='k';
						ret+=atkPiece;
					}
					else blackUndoMove(tem,i,'k',atkPiece);
				}
			}
		}
		
		//Bishop1
		if(blackBishop1pos != -1){
			long blackBishop1posible = blackBishopMoves(blackBishop1pos);
			for(int i=63;i>=0;i--) {
				if( (blackBishop1posible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackBishop1pos;
					blackMakeMove(blackBishop1pos,i,'B',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'B',atkPiece);
						if(blackBishop1pos < 10) ret+='0';
						ret+=blackBishop1pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='B';
						ret+=atkPiece;
					}
					else blackUndoMove(tem,i,'B',atkPiece);
				}
			}
		}
		
		//Bishop2
		if(blackBishop2pos != -1){
			long blackBishop2posible = blackBishopMoves(blackBishop2pos);
			for(int i=63;i>=0;i--) {
				if( (blackBishop2posible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackBishop2pos;
					blackMakeMove(blackBishop2pos,i,'b',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'b',atkPiece);
						if(blackBishop2pos < 10) ret+='0';
						ret+=blackBishop2pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='b';
						ret+=atkPiece;
					}
					else blackUndoMove(tem,i,'b',atkPiece);
				}
			}
		}
		
		//Rook1
		if(blackRook1pos != -1){
			long blackRook1posible = blackRookMoves(blackRook1pos);
			for(int i=63;i>=0;i--) {
				if( (blackRook1posible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackRook1pos;
					blackMakeMove(blackRook1pos,i,'R',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'R',atkPiece);
						if(blackRook1pos < 10) ret+='0';
						ret+=blackRook1pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='R';
						ret+=atkPiece;
					}
					else blackUndoMove(tem,i,'R',atkPiece);
				}
			}
		}
		
		//Rook2
		if(blackRook2pos != -1){
			long blackRook2posible = blackRookMoves(blackRook2pos);
			for(int i=63;i>=0;i--) {
				if( (blackRook2posible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackRook2pos;
					blackMakeMove(blackRook2pos,i,'r',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'r',atkPiece);
						if(blackRook2pos < 10) ret+='0';
						ret+=blackRook2pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='r';
						ret+=atkPiece;
					}
					else blackUndoMove(tem,i,'r',atkPiece);
				}
			}
		}
		
		//Queen 
		if(blackQueenpos != -1){
			long blackQueenposible = blackQueenMoves(blackQueenpos);
			for(int i=63;i>=0;i--) {
				if( (blackQueenposible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackQueenpos;
					blackMakeMove(blackQueenpos,i,'Q',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'Q',atkPiece);
						if(blackQueenpos < 10) ret+='0';
						ret+=blackQueenpos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='Q';
						ret+=atkPiece;
					}
					else blackUndoMove(tem,i,'Q',atkPiece);
				}
			}
		}
		
		
		//king
		if(blackKingpos != -1){
			long blackKingposible = blackKingMoves(blackKingpos);
			for(int i=63;i>=0;i--) {
				if( (blackKingposible & (1L<<i)) > 0L) {
					char atkPiece = getWhiteAttacked(i); ///CHANGED
					
					int tem = blackKingpos;
					blackMakeMove(blackKingpos,i,'A',atkPiece);
					if(blackKingSafe()){
						blackUndoMove(tem,i,'A',atkPiece);
						if(blackKingpos < 10) ret+='0';
						ret+=blackKingpos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='A';
						ret+=atkPiece;
					}	
					else blackUndoMove(tem,i,'A',atkPiece);
				}
			}
		}
		
		return ret;
		
	}
	
	
	public static void whiteMakeMove(int st,int en,char piece,char atkPiece){
		
		if(atkPiece != ' ') {
			blackPieces ^= (1L<<en);
			if(atkPiece == 'P') {
				blackPawns ^= (1L<<en);
				zoboristHash ^= zoboristValue[5][en];
			}
			else if(atkPiece == 'K') {blackKnight1pos = -1;zoboristHash ^= zoboristValue[3][en];}
			else if(atkPiece == 'k') {blackKnight2pos = -1;zoboristHash ^= zoboristValue[3][en];}
			else if(atkPiece == 'B') {blackBishop1pos = -1;zoboristHash ^= zoboristValue[4][en];}
			else if(atkPiece == 'b') {blackBishop2pos = -1;zoboristHash ^= zoboristValue[4][en];}
			else if(atkPiece == 'R') {blackRook1pos = -1;zoboristHash ^= zoboristValue[2][en];}
			else if(atkPiece == 'r') {blackRook2pos = -1;zoboristHash ^= zoboristValue[2][en];}
			else {blackQueenpos = -1;zoboristHash ^= zoboristValue[1][en];}
		}
		
		whitePieces ^= (1L<<st);
		whitePieces |= (1L<<en);
		if(piece == 'P'){
			whitePawns ^= (1L<<st);
			whitePawns |= (1L<<en);
			zoboristHash ^= zoboristValue[11][st];
			zoboristHash ^= zoboristValue[11][en];
		}
		else if(piece == 'A') {whiteKingpos = en;zoboristHash ^= zoboristValue[6][st];
			zoboristHash ^= zoboristValue[6][en];}
			
		else if(piece == 'K') {whiteKnight1pos = en;zoboristHash ^= zoboristValue[9][st];
			zoboristHash ^= zoboristValue[9][en];}
		else if(piece == 'k') {whiteKnight2pos = en;zoboristHash ^= zoboristValue[9][st];
			zoboristHash ^= zoboristValue[9][en];}
			
		else if(piece == 'B') {whiteBishop1pos = en;zoboristHash ^= zoboristValue[10][st];
			zoboristHash ^= zoboristValue[10][en];}
		else if(piece == 'b') {whiteBishop2pos = en;zoboristHash ^= zoboristValue[10][st];
			zoboristHash ^= zoboristValue[10][en];}
			
		else if(piece == 'R') {whiteRook1pos = en;zoboristHash ^= zoboristValue[8][st];
			zoboristHash ^= zoboristValue[8][en];}
		else if(piece == 'r') {whiteRook2pos = en;zoboristHash ^= zoboristValue[8][st];
			zoboristHash ^= zoboristValue[8][en];}
			
		else {whiteQueenpos = en;zoboristHash ^= zoboristValue[7][st];
			zoboristHash ^= zoboristValue[7][en];}
			
	}
	
	public static void whiteUndoMove(int st,int en,char piece,char atkPiece){
		
		if(atkPiece != ' ') {
			blackPieces |= (1L<<en);
			if(atkPiece == 'P') {
				blackPawns |= (1L<<en);
				zoboristHash ^= zoboristValue[5][en];
			}
			else if(atkPiece == 'K') {blackKnight1pos = en;zoboristHash ^= zoboristValue[3][en];}
			else if(atkPiece == 'k') {blackKnight2pos = en;zoboristHash ^= zoboristValue[3][en];}
			else if(atkPiece == 'B') {blackBishop1pos = en;zoboristHash ^= zoboristValue[4][en];}
			else if(atkPiece == 'b') {blackBishop2pos = en;zoboristHash ^= zoboristValue[4][en];}
			else if(atkPiece == 'R') {blackRook1pos = en;zoboristHash ^= zoboristValue[2][en];}
			else if(atkPiece == 'r') {blackRook2pos = en;zoboristHash ^= zoboristValue[2][en];}
			else {blackQueenpos = en;zoboristHash ^= zoboristValue[1][en];}
		}
		
		whitePieces |= (1L<<st);
		whitePieces ^= (1L<<en);
		if(piece == 'P'){
			whitePawns |= (1L<<st);
			whitePawns ^= (1L<<en);
			zoboristHash ^= zoboristValue[11][st];
			zoboristHash ^= zoboristValue[11][en];
		}
		else if(piece == 'A') {whiteKingpos = st;zoboristHash ^= zoboristValue[6][st];
			zoboristHash ^= zoboristValue[6][en];}
			
		else if(piece == 'K') {whiteKnight1pos = st;zoboristHash ^= zoboristValue[9][st];
			zoboristHash ^= zoboristValue[9][en];}
		else if(piece == 'k') {whiteKnight2pos = st;zoboristHash ^= zoboristValue[9][st];
			zoboristHash ^= zoboristValue[9][en];}
			
		else if(piece == 'B') {whiteBishop1pos = st;zoboristHash ^= zoboristValue[10][st];
			zoboristHash ^= zoboristValue[10][en];}
		else if(piece == 'b') {whiteBishop2pos = st;zoboristHash ^= zoboristValue[10][st];
			zoboristHash ^= zoboristValue[10][en];}
			
		else if(piece == 'R') {whiteRook1pos = st;zoboristHash ^= zoboristValue[8][st];
			zoboristHash ^= zoboristValue[8][en];}
		else if(piece == 'r') {whiteRook2pos = st;zoboristHash ^= zoboristValue[8][st];
			zoboristHash ^= zoboristValue[8][en];}
			
		else {whiteQueenpos = st;zoboristHash ^= zoboristValue[7][st];
			zoboristHash ^= zoboristValue[7][en];}
			
	}
	
	public static char getBlackAttacked(int pos){
		if(blackKnight1pos == pos) return 'K';
		if(blackKnight2pos == pos) return 'k';
		
		if(blackBishop1pos == pos) return 'B';
		if(blackBishop2pos == pos) return 'b';
		
		if(blackRook1pos == pos) return 'R';
		if(blackRook2pos == pos) return 'r';
		
		if(blackQueenpos == pos) return 'Q';
		if(blackKingpos == pos) return 'A';
		if((blackPawns & (1L<<pos) )>0L) return 'P';
		return ' ';
	}
	
	public static boolean whitePlaysMove(int startPosFind,int endPosFind){
		long startPos = 1L<<startPosFind;
		if((startPos & whitePieces) == 0L) return false;
		long endPos = 1L<<endPosFind;
		
		char atkPiece = getBlackAttacked(endPosFind);
		/*if(blackKnight1pos == endPosFind) atkPiece = 'K';
		else if(blackKnight2pos == endPosFind) atkPiece = 'k';
		else if( (blackPawns & endPos ) > 0L) atkPiece = 'P';
		else atkPiece = ' ';*/
		//king
		if(startPosFind == whiteKingpos){
			if((whiteKingMoves(whiteKingpos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'A',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'A',atkPiece);
				return false;
			}
			return false;
		}
		//knight
		if(startPosFind == whiteKnight1pos){
			//System.out.println("HELLO");
			//System.out.println(Long.toBinaryString(whiteKnightMoves(whiteKnight1pos)));
			if((whiteKnightMoves(whiteKnight1pos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'K',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'K',atkPiece);
				return false;
			}
			return false;
		}
		
		
		if(startPosFind == whiteKnight2pos){
			if((whiteKnightMoves(whiteKnight2pos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'k',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'k',atkPiece);
				return false;
			}
			return false;
		}
		
		//Bishop
		if(startPosFind == whiteBishop1pos){
			if((whiteBishopMoves(whiteBishop1pos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'B',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'B',atkPiece);
				return false;
			}
			return false;
		}
		if(startPosFind == whiteBishop2pos){
			if((whiteBishopMoves(whiteBishop2pos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'b',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'b',atkPiece);
				return false;
			}
			return false;
		}
		
		//Rook
		if(startPosFind == whiteRook1pos){
			if((whiteRookMoves(whiteRook1pos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'R',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'R',atkPiece);
				return false;
			}
			return false;
		}
		if(startPosFind == whiteRook2pos){
			if((whiteRookMoves(whiteRook2pos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'r',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'r',atkPiece);
				return false;
			}
			return false;
		}
		
		//Queen
		if(startPosFind == whiteQueenpos){
			if((whiteQueenMoves(whiteQueenpos) & endPos) > 0L) {
				whiteMakeMove(startPosFind,endPosFind,'Q',atkPiece);
				if(whiteKingSafe()) return true;
				whiteUndoMove(startPosFind,endPosFind,'Q',atkPiece);
				return false;
			}
			return false;
		}
		
		//pawn
		if((endPos & (whitePawnAttacks(startPos) | whitePawnForward(startPos) ) ) > 0L){		
			whiteMakeMove(startPosFind,endPosFind,'P',atkPiece);
			if(whiteKingSafe()) return true;
			whiteUndoMove(startPosFind,endPosFind,'P',atkPiece);
			return false;
		}
		return false;
		
	}
	
	public static String whiteCheckMoves(){
		String ret = "";
		
		//pawns fwd
		long fwd = whitePawnForward(whitePawns);
		for(int i=63;i>=0;i--) {
			if( (fwd & (1L<<i)) > 0L) {
				int off = 16;
				if( (whitePawns&(1L<<(i-8))) > 0L) off = 8;
			
				whiteMakeMove(i-off,i,'P',' ');
				if(whiteKingSafe()){
					whiteUndoMove(i-off,i,'P',' ');
					//store
					if(i-off < 10) ret+='0';
					ret+=(i-off);
					if(i<10) ret+='0';
					ret+=i;
					ret+="P ";
				}
				else whiteUndoMove(i-off,i,'P',' ');
			}
		}
		
		//pawns attack
		long atk = whitePawnAttacks(whitePawns);
		for(int i=63;i>=0;i--) {
			if( (atk & (1L<<i)) > 0L) {

				char atkPiece = getBlackAttacked(i);

				if(i%8!=0 && ((whitePawns&(1L<<(i-9)))>0L) ){
					whiteMakeMove(i-9,i,'P',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(i-9,i,'P',atkPiece);
						if(i-9 < 10) ret+='0';
						ret+=(i-9);
						if(i<10) ret+='0';
						ret+=i;
						ret+='P';
						ret+=atkPiece;
					}
					else whiteUndoMove(i-9,i,'P',atkPiece);
					
				}
				
				if(i%8!=7 && ((whitePawns&(1L<<(i-7))) > 0L)){
					
					whiteMakeMove(i-7,i,'P',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(i-7,i,'P',atkPiece);
						if(i-7 < 10) ret+='0';
						ret+=(i-7);
						if(i<10) ret+='0';
						ret+=i;
						ret+='P';
						ret+=atkPiece;
					}
					else whiteUndoMove(i-7,i,'P',atkPiece);
				}
			}
		}
		
		
		//knight1
		if(whiteKnight1pos != -1){
			long whiteKnight1posible = whiteKnightMoves(whiteKnight1pos);
			for(int i=63;i>=0;i--) {
				if( (whiteKnight1posible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteKnight1pos;
					whiteMakeMove(whiteKnight1pos,i,'K',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'K',atkPiece);
						if(whiteKnight1pos < 10) ret+='0';
						ret+=whiteKnight1pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='K';
						ret+=atkPiece;
					}
					else whiteUndoMove(tem,i,'K',atkPiece);
				}
			}
		}
		
		//knight2
		if(whiteKnight2pos != -1){
			long whiteKnight2posible = whiteKnightMoves(whiteKnight2pos);
			for(int i=63;i>=0;i--) {
				if( (whiteKnight2posible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteKnight2pos;
					whiteMakeMove(whiteKnight2pos,i,'k',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'k',atkPiece);
						if(whiteKnight2pos < 10) ret+='0';
						ret+=whiteKnight2pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='k';
						ret+=atkPiece;
					}
					else whiteUndoMove(tem,i,'k',atkPiece);
				}
			}
		}
		
		//Bishop1
		if(whiteBishop1pos != -1){
			long whiteBishop1posible = whiteBishopMoves(whiteBishop1pos);
			for(int i=63;i>=0;i--) {
				if( (whiteBishop1posible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteBishop1pos;
					whiteMakeMove(whiteBishop1pos,i,'B',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'B',atkPiece);
						if(whiteBishop1pos < 10) ret+='0';
						ret+=whiteBishop1pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='B';
						ret+=atkPiece;
					}
					else whiteUndoMove(tem,i,'B',atkPiece);
				}
			}
		}
		
		//Bishop2
		if(whiteBishop2pos != -1){
			long whiteBishop2posible = whiteBishopMoves(whiteBishop2pos);
			for(int i=63;i>=0;i--) {
				if( (whiteBishop2posible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteBishop2pos;
					whiteMakeMove(whiteBishop2pos,i,'b',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'b',atkPiece);
						if(whiteBishop2pos < 10) ret+='0';
						ret+=whiteBishop2pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='b';
						ret+=atkPiece;
					}
					else whiteUndoMove(tem,i,'b',atkPiece);
				}
			}
		}
		
		//Rook1
		if(whiteRook1pos != -1){
			long whiteRook1posible = whiteRookMoves(whiteRook1pos);
			for(int i=63;i>=0;i--) {
				if( (whiteRook1posible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteRook1pos;
					whiteMakeMove(whiteRook1pos,i,'R',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'R',atkPiece);
						if(whiteRook1pos < 10) ret+='0';
						ret+=whiteRook1pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='R';
						ret+=atkPiece;
					}
					else whiteUndoMove(tem,i,'R',atkPiece);
				}
			}
		}
		
		//Rook2
		if(whiteRook2pos != -1){
			long whiteRook2posible = whiteRookMoves(whiteRook2pos);
			for(int i=63;i>=0;i--) {
				if( (whiteRook2posible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteRook2pos;
					whiteMakeMove(whiteRook2pos,i,'r',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'r',atkPiece);
						if(whiteRook2pos < 10) ret+='0';
						ret+=whiteRook2pos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='r';
						ret+=atkPiece;
					}
					else whiteUndoMove(tem,i,'r',atkPiece);
				}
			}
		}
		
		//Queen 
		if(whiteQueenpos != -1){
			long whiteQueenposible = whiteQueenMoves(whiteQueenpos);
			for(int i=63;i>=0;i--) {
				if( (whiteQueenposible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteQueenpos;
					whiteMakeMove(whiteQueenpos,i,'Q',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'Q',atkPiece);
						if(whiteQueenpos < 10) ret+='0';
						ret+=whiteQueenpos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='Q';
						ret+=atkPiece;
					}
					else whiteUndoMove(tem,i,'Q',atkPiece);
				}
			}
		}
		
		
		//king
		if(whiteKingpos != -1){
			long whiteKingposible = whiteKingMoves(whiteKingpos);
			for(int i=63;i>=0;i--) {
				if( (whiteKingposible & (1L<<i)) > 0L) {
					char atkPiece = getBlackAttacked(i); ///CHANGED
					
					int tem = whiteKingpos;
					whiteMakeMove(whiteKingpos,i,'A',atkPiece);
					if(whiteKingSafe()){
						whiteUndoMove(tem,i,'A',atkPiece);
						if(whiteKingpos < 10) ret+='0';
						ret+=whiteKingpos;
						if(i<10) ret+='0';
						ret+=i;
						ret+='A';
						ret+=atkPiece;
					}	
					else whiteUndoMove(tem,i,'A',atkPiece);
				}
			}
		}
		
		return ret;
		
	}
	
		
	public static long blackKnightMoves(int position){
		return knightMask[position] & ~blackPieces;
	}
	
	public static long whiteKnightMoves(int position){
		
		//System.out.println(position + " HEY " + Long.toBinaryString(knightMask[position]));
		return knightMask[position] & ~whitePieces;
	}
	
	public static long blackRookMoves(int position){
		//System.out.println(position + " HEY " + Long.toBinaryString(HorizandVertMoves(position)));
		return HorizandVertMoves(position) & ~blackPieces;
	}
	
	public static long whiteRookMoves(int position){
		return HorizandVertMoves(position) & ~whitePieces;
	}
	
	
	public static long blackBishopMoves(int position){
		return DiagandAntidiagMoves(position) & ~blackPieces;
	}
	
	public static long whiteBishopMoves(int position){
		
		return DiagandAntidiagMoves(position) & ~whitePieces;
	}
	
	public static long blackQueenMoves(int position){
		return (DiagandAntidiagMoves(position) | HorizandVertMoves(position)) & ~blackPieces;
	}
	
	public static long whiteQueenMoves(int position){
		
		return (DiagandAntidiagMoves(position) | HorizandVertMoves(position)) & ~whitePieces;
	}
	
	
	
	public static long blackKingMoves(int position){
		return kingMask[position] & ~blackPieces;
	}
	
	public static long whiteKingMoves(int position){
		return kingMask[position] & ~whitePieces;
	}
	
	
	public static long blackPawnForward(long mask){
		long ret = mask >>> 8;
		ret = ret & ~(whitePieces|blackPieces);
		
		mask = mask & (255L << 48) ;
		long push2 = mask >>> 8;
		push2 = push2 & ~(whitePieces|blackPieces);
		push2 = push2 >>> 8;
		push2 = push2 & ~(whitePieces|blackPieces);
		
		return ret | push2;
	}
	
	public static long whitePawnForward(long mask){
		long ret = mask << 8;
		ret = ret & ~(whitePieces|blackPieces);
		
		mask = mask & (255L << 8) ;
		long push2 = mask << 8;
		push2 = push2 & ~(whitePieces|blackPieces);
		push2 = push2 << 8;
		push2 = push2 & ~(whitePieces|blackPieces);
		
		return ret | push2;
	}
	
	static long rmvFileA = 0b0111111101111111011111110111111101111111011111110111111101111111L;
	static long rmvFileH = 0b1111111011111110111111101111111011111110111111101111111011111110L;
	
	public static long blackPawnAttacks(long mask){
		
		long ret = 0L;
		
		long left = mask & rmvFileA;
		long right = mask & rmvFileH;
		ret = ((left>>>7)|(right>>>9))&whitePieces;
		
		/*for(int i=63;i>=0;i--) {
			if( (mask & (1L<<i)) > 0L) {
				ret |= blackPawnMask[i] & ~blackPieces;
			}
		}*/
		return ret;
	}
	
	public static long whitePawnAttacks(long mask){
		long ret = 0L;
		long left = mask & rmvFileA;
		long right = mask & rmvFileH;
		ret = ((left<<9)|(right<<7))&blackPieces;
		/*for(int i=63;i>=0;i--) {
			if( (mask & (1L<<i)) > 0L) {
				ret |= whitePawnMask[i] & ~whitePieces;
			}
		}*/
		return ret;
	}
	
	public static boolean blackKingSafe() {
		long kingPos = (1L<<blackKingpos);
		
		if(whiteKnight1pos!=-1 && (kingPos & whiteKnightMoves(whiteKnight1pos)) != 0L) return false;
		if(whiteKnight2pos!=-1 && (kingPos & whiteKnightMoves(whiteKnight2pos)) != 0L) return false;
		
		if(whiteRook1pos!=-1 && (kingPos & whiteRookMoves(whiteRook1pos)) != 0L) return false;
		if(whiteRook2pos!=-1 && (kingPos & whiteRookMoves(whiteRook2pos)) != 0L) return false;
		
		if(whiteBishop1pos!=-1 && (kingPos & whiteBishopMoves(whiteBishop1pos)) != 0L) return false;
		if(whiteBishop2pos!=-1 && (kingPos & whiteBishopMoves(whiteBishop2pos)) != 0L) return false;
		
		if(whiteQueenpos!=-1 && (kingPos & whiteQueenMoves(whiteQueenpos)) != 0L) return false;
		
		
		if(whiteKingpos!=-1 && (kingPos & whiteKingMoves(whiteKingpos)) != 0L) return false;
		if((kingPos & whitePawnAttacks(whitePawns)) != 0L) return false;
		
		return true;
	}

	public static boolean whiteKingSafe() {
		long kingPos = (1L<<whiteKingpos);
		if(blackKnight1pos!=-1 && (kingPos & blackKnightMoves(blackKnight1pos)) != 0L) return false;
		if(blackKnight2pos!=-1 && (kingPos & blackKnightMoves(blackKnight2pos)) != 0L) return false;
		
		
		if(blackRook1pos!=-1 && (kingPos & blackRookMoves(blackRook1pos)) != 0L) return false;
		if(blackRook2pos!=-1 && (kingPos & blackRookMoves(blackRook2pos)) != 0L) return false;
		
		if(blackBishop1pos!=-1 && (kingPos & blackBishopMoves(blackBishop1pos)) != 0L) return false;
		if(blackBishop2pos!=-1 && (kingPos & blackBishopMoves(blackBishop2pos)) != 0L) return false;
		
		if(blackQueenpos!=-1 && (kingPos & blackQueenMoves(blackQueenpos)) != 0L) return false;
		
		if(blackKingpos!=-1 && (kingPos & blackKingMoves(blackKingpos)) != 0L) return false;
		if((kingPos & blackPawnAttacks(blackPawns)) != 0L) return false;
		return true;
	}	
	
}