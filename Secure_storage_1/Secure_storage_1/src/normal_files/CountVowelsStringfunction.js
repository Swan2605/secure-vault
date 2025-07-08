function countVowels(str1)
{
    var count=0;
    for(let i in str1)
    {
        if(i=='a'||'e'||'i'||'o'||'u')
        {
            
            count++;
        }
    }
    console.log("The count of vowels is :",count);
}

countVowels("This is a Vowel String")
/*
const val=countVowels("This is a string");

console.log("The Number of Vowels is :",val);*/