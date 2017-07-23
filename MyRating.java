import java.util.*;
import javafx.util.Pair;
import javax.swing.*;
public class MyRating{
	
	static int pawnBoard[][]={//attribute to http://chessprogramming.wikispaces.com/Simplified+evaluation+function
        { 0,  0,  0,  0,  0,  0,  0,  0},
        {50, 50, 50, 50, 50, 50, 50, 50},
        {10, 10, 20, 30, 30, 20, 10, 10},
        { 5,  5, 10, 25, 25, 10,  5,  5},
        { 0,  0,  0, 20, 20,  0,  0,  0},
        { 5, -5,-10,  0,  0,-10, -5,  5},
        { 5, 10, 10,-20,-20, 10, 10,  5},
        { 0,  0,  0,  0,  0,  0,  0,  0}};
    static int rookBoard[][]={
        { 0,  0,  0,  0,  0,  0,  0,  0},
        { 5, 10, 10, 10, 10, 10, 10,  5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        {-5,  0,  0,  0,  0,  0,  0, -5},
        { 0,  0,  0,  5,  5,  0,  0,  0}};
    static int knightBoard[][]={
        {-50,-40,-30,-30,-30,-30,-40,-50},
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  0, 15, 20, 20, 15,  0,-30},
        {-30,  5, 10, 15, 15, 10,  5,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40},
        {-50,-40,-30,-30,-30,-30,-40,-50}};
    static int bishopBoard[][]={
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10},
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10},
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10},
        {-20,-10,-10,-10,-10,-10,-10,-20}};
    static int queenBoard[][]={
        {-20,-10,-10, -5, -5,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5,  5,  5,  5,  0,-10},
        { -5,  0,  5,  5,  5,  5,  0, -5},
        {  0,  0,  5,  5,  5,  5,  0, -5},
        {-10,  5,  5,  5,  5,  5,  0,-10},
        {-10,  0,  5,  0,  0,  0,  0,-10},
        {-20,-10,-10, -5, -5,-10,-10,-20}};
    static int kingMidBoard[][]={
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20},
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20},
        { 20, 30, 10,  0,  0, 10, 30, 20}};
    static int kingEndBoard[][]={
        {-50,-40,-30,-20,-20,-30,-40,-50},
        {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30},
        {-50,-30,-30,-30,-30,-30,-30,-50}};
	
	public static int RateBlack(int depth,int listlen) {
		int ret = listlen;
		int materialBlack = MaterialRateBlack();
		int materialWhite = MaterialRateWhite();
		ret+=materialBlack;
		ret-=materialWhite;
		ret+=AttackplusPosRateBlack(materialBlack);
		ret-=AttackplusPosRateWhite(materialWhite);
		
		return ret + depth*50;
    }
	public static int AttackplusPosRateBlack(int materialBlack){
		int tot=0;
		long mask=MyEngine.blackPieces;
		int act=MyEngine.blackKingpos;
		while(mask>0L){
			long cur=mask&(~(mask-1));
			int pos = Long.numberOfTrailingZeros(cur);
			MyEngine.blackKingpos = pos;
			boolean isKingSafe = MyEngine.blackKingSafe();
			int i=pos/8,j=pos%8;
			
			if(MyEngine.blackQueenpos == pos)  {
				if(!isKingSafe) tot-=450;
				tot+=queenBoard[i][j];
			}
			else if(MyEngine.blackRook1pos == pos)  {
				if(!isKingSafe) tot-=250;
				tot+=rookBoard[i][j];
			}
			else if(MyEngine.blackRook2pos == pos)  {
				if(!isKingSafe) tot-=250;
				tot+=rookBoard[i][j];
			}
			else if(MyEngine.blackKnight1pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=knightBoard[i][j];
			}
			else if(MyEngine.blackKnight2pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=knightBoard[i][j];
			}
			else if(MyEngine.blackBishop1pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=bishopBoard[i][j];
			}
			else if(MyEngine.blackBishop2pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=bishopBoard[i][j];
			}
			else if(act == pos) {
				if(!isKingSafe) tot-=100;
				if(materialBlack>=1750) tot+=kingMidBoard[i][j];
				else tot+=kingEndBoard[i][j];
			}
			else {
				if(!isKingSafe) tot-=32;
				tot+=pawnBoard[i][j];
			}
		
			mask&=(mask-1);
		}
		MyEngine.blackKingpos = act;
		return tot;
	}
	
	
	public static int AttackplusPosRateWhite(int materialWhite){
		int tot=0;
		long mask=MyEngine.whitePieces;
		int act=MyEngine.whiteKingpos;
		while(mask>0L){
			long cur=mask&(~(mask-1));
			int pos = Long.numberOfTrailingZeros(cur);
			MyEngine.whiteKingpos = pos;
			boolean isKingSafe = MyEngine.whiteKingSafe();
			int i=(63-pos)/8,j=(63-pos)%8;
			
			if(MyEngine.whiteQueenpos == pos)  {
				if(!isKingSafe) tot-=450;
				tot+=queenBoard[i][j];
			}
			else if(MyEngine.whiteRook1pos == pos)  {
				if(!isKingSafe) tot-=250;
				tot+=rookBoard[i][j];
			}
			else if(MyEngine.whiteRook2pos == pos)  {
				if(!isKingSafe) tot-=250;
				tot+=rookBoard[i][j];
			}
			else if(MyEngine.whiteKnight1pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=knightBoard[i][j];
			}
			else if(MyEngine.whiteKnight2pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=knightBoard[i][j];
			}
			else if(MyEngine.whiteBishop1pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=bishopBoard[i][j];
			}
			else if(MyEngine.whiteBishop2pos == pos)  {
				if(!isKingSafe) tot-=150;
				tot+=bishopBoard[i][j];
			}
			else if(act == pos) {
				if(!isKingSafe) tot-=100;
				if(materialWhite>=1750) tot+=kingMidBoard[i][j];
				else tot+=kingEndBoard[i][j];
			}
			else {
				if(!isKingSafe) tot-=32;
				tot+=pawnBoard[i][j];
			}
		
			mask&=(mask-1);
		}
		MyEngine.whiteKingpos = act;
		return tot;
	}
		
	//blocked pawns
	public static int MaterialRateBlack(){
		int tot=0;
		long mask=MyEngine.blackPawns;
		tot-=100*Long.bitCount(mask);
		
		if(MyEngine.blackQueenpos!=-1)  tot-=900;
		if(MyEngine.blackRook1pos!=-1)  tot-=500;
		if(MyEngine.blackRook2pos!=-1)  tot-=500;
		if(MyEngine.blackKnight1pos!=-1)  tot-=300;
		if(MyEngine.blackKnight2pos!=-1)  tot-=300;
			
		if(MyEngine.blackBishop1pos!=-1 && MyEngine.blackBishop2pos!=-1) tot-=600;
		else if(MyEngine.blackBishop1pos!=-1 || MyEngine.blackBishop2pos!=-1) tot-=250;
		return -tot;
	}
	
	public static int MaterialRateWhite(){
		int tot=0;
		long mask=MyEngine.whitePawns;
		tot+=100*Long.bitCount(mask);
		
		if(MyEngine.whiteQueenpos!=-1)  tot+=900;
		if(MyEngine.whiteRook1pos!=-1)  tot+=500;
		if(MyEngine.whiteRook2pos!=-1)  tot+=500;
		if(MyEngine.whiteKnight1pos!=-1)  tot+=300;
		if(MyEngine.whiteKnight2pos!=-1)  tot+=300;
		
		if(MyEngine.whiteBishop1pos!=-1 && MyEngine.whiteBishop2pos!=-1) tot+=600;
		else if(MyEngine.whiteBishop1pos!=-1 || MyEngine.whiteBishop2pos!=-1) tot+=250;
		
		return tot;
	}

}